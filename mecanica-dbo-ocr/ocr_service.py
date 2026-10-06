#!/usr/bin/env python3
"""
Mecânica DBO — Microserviço OCR
Lê notas fiscais térmicas via imagem e extrai itens automaticamente
Porta: 5000
"""

import os
import re
import shutil
import json
import base64
import traceback
from io import BytesIO
from datetime import datetime

import cv2
import numpy as np
import pytesseract
from PIL import Image
from flask import Flask, request, jsonify

# ── Configuração do Tesseract ─────────────────────────────────────────────────
# Ordem: variável TESSERACT_CMD > instalação padrão do Windows > PATH do sistema.
_WIN = r"C:\Program Files\Tesseract-OCR\tesseract.exe"
_cmd = os.environ.get("TESSERACT_CMD") or (_WIN if os.path.exists(_WIN) else shutil.which("tesseract"))
if _cmd:
    pytesseract.pytesseract.tesseract_cmd = _cmd


def _idiomas() -> str:
    """Usa português + inglês; se o pacote 'por' não estiver instalado, cai para inglês."""
    try:
        disponiveis = set(pytesseract.get_languages(config=""))
    except Exception:
        return "eng"
    return "por+eng" if "por" in disponiveis else "eng"


IDIOMAS = _idiomas()

app = Flask(__name__)

# ── Pré-processamento de imagem ───────────────────────────────────────────────
def preprocessar_imagem(img_array: np.ndarray) -> np.ndarray:
    """
    Prepara a imagem para OCR:
    1. Converte para escala de cinza
    2. Aumenta o contraste
    3. Binariza (preto e branco)
    4. Remove ruído
    5. Redimensiona se necessário
    """
    # Converte BGR → Cinza
    if len(img_array.shape) == 3:
        gray = cv2.cvtColor(img_array, cv2.COLOR_BGR2GRAY)
    else:
        gray = img_array

    # Redimensiona se a imagem for muito pequena
    h, w = gray.shape
    if w < 800:
        scale = 800 / w
        gray = cv2.resize(gray, None, fx=scale, fy=scale,
                          interpolation=cv2.INTER_CUBIC)

    # Aumenta contraste com CLAHE
    clahe = cv2.createCLAHE(clipLimit=2.0, tileGridSize=(8, 8))
    gray = clahe.apply(gray)

    # Binarização adaptativa (lida bem com iluminação irregular)
    binario = cv2.adaptiveThreshold(
        gray, 255,
        cv2.ADAPTIVE_THRESH_GAUSSIAN_C,
        cv2.THRESH_BINARY, 31, 10
    )

    # Remove ruído com kernel pequeno
    kernel = np.ones((1, 1), np.uint8)
    binario = cv2.morphologyEx(binario, cv2.MORPH_CLOSE, kernel)

    return binario


# ── Extração de texto via Tesseract ──────────────────────────────────────────
def extrair_texto(img_array: np.ndarray) -> str:
    """Roda o Tesseract na imagem preprocessada."""
    img_proc = preprocessar_imagem(img_array)
    pil_img = Image.fromarray(img_proc)

    # Sem whitelist de caracteres: o motor LSTM quase não a respeita e, escrita com
    # espaço dentro, o pytesseract a quebra em argumentos e o OCR perde os espaços
    # entre as palavras ("FILTRODEOLEO").
    config = f"--psm 6 --oem 3 -l {IDIOMAS}"

    texto = pytesseract.image_to_string(pil_img, config=config)
    return texto


# ── Parser de itens da nota fiscal ───────────────────────────────────────────
_PADRAO_QTD = re.compile(r'\b(\d{1,3})\s*(?:UN|PC|PCS|KG|LT|L)\b', re.I)


def separar_quantidade(descricao: str):
    """'PASTILHA FREIO 2 UN' -> ('PASTILHA FREIO', 2). Sem unidade na linha, quantidade 1."""
    m = _PADRAO_QTD.search(descricao)
    if not m or int(m.group(1)) <= 0:
        return descricao, 1
    limpa = _PADRAO_QTD.sub('', descricao, count=1)
    limpa = re.sub(r'\s{2,}', ' ', limpa).strip()
    return (limpa if len(limpa) >= 3 else descricao), int(m.group(1))

def parsear_itens(texto: str) -> list:
    """
    Extrai itens, quantidades e valores do texto da NF.
    Retorna lista de dicts: {descricao, quantidade, valor_unitario, valor_total}
    """
    itens = []
    linhas = texto.split('\n')

    # Padrão: qualquer linha com valor monetário no formato X X,XX ou X.XXX,XX
    padrao_valor = re.compile(
        r'(.+?)\s+'           # descrição
        r'(\d+)\s*'           # quantidade (opcional)
        r'[xX*]?\s*'
        r'(\d{1,3}(?:\.\d{3})*(?:,\d{2})?)\s*'  # valor unitário
        r'(\d{1,3}(?:\.\d{3})*(?:,\d{2})?)?'    # valor total (opcional)
    )

    # Padrão simples: linha + valor no final
    padrao_simples = re.compile(
        r'^(.+?)\s+'
        r'(\d{1,3}(?:\.\d{3})*,\d{2})\s*$'
    )

    for linha in linhas:
        linha = linha.strip()
        if len(linha) < 5:
            continue

        # Ignora linhas de cabeçalho/rodapé comuns
        ignorar = ['cnpj', 'cpf', 'nota fiscal', 'nf-e', 'data', 'hora',
                   'total', 'troco', 'subtotal', 'desconto', 'acrescimo',
                   'forma de pagamento', 'pix', 'dinheiro', 'cartao',
                   'protocolo', 'autorizacao', 'serie', 'numero']
        if any(p in linha.lower() for p in ignorar):
            continue

        # Tenta padrão simples (descrição + valor)
        m = padrao_simples.match(linha)
        # com dois valores na linha (unitário e total), quem trata é o caminho de baixo
        if m and not re.search(r'\d{1,3}(?:\.\d{3})*,\d{2}', m.group(1)):
            descricao = m.group(1).strip()
            valor_str = m.group(2)
            valor = float(valor_str.replace('.', '').replace(',', '.'))

            descricao, qtd = separar_quantidade(descricao)
            if len(descricao) >= 3 and valor > 0:
                itens.append({
                    "descricao": descricao[:200],
                    "quantidade": qtd,
                    "valor_unitario": round(valor / qtd, 2),
                    "valor_total": valor,
                    "pago_pelo_cliente": False,
                    "origem": "ocr"
                })
                continue

        # Tenta encontrar valores monetários na linha
        valores = re.findall(r'\d{1,3}(?:\.\d{3})*,\d{2}', linha)
        if valores and len(linha) > 10:
            # Remove os valores da descrição
            descricao = re.sub(r'\d{1,3}(?:\.\d{3})*,\d{2}', '', linha).strip()
            descricao = re.sub(r'\s{2,}', ' ', descricao).strip()

            if len(descricao) < 3:
                continue

            valor_str = valores[-1]  # Último valor = total
            valor = float(valor_str.replace('.', '').replace(',', '.'))

            descricao, qtd = separar_quantidade(descricao)

            if valor > 0 and len(descricao) >= 3:
                itens.append({
                    "descricao": descricao[:200],
                    "quantidade": qtd,
                    "valor_unitario": round(valor / qtd, 2),
                    "valor_total": valor,
                    "pago_pelo_cliente": False,
                    "origem": "ocr"
                })

    return itens


# ── Extração de metadados da NF ───────────────────────────────────────────────
def extrair_metadados(texto: str) -> dict:
    """Extrai CNPJ, número da NF, data e valor total do texto."""
    meta = {}

    # CNPJ
    cnpj = re.search(r'\d{2}\.\d{3}\.\d{3}/\d{4}-\d{2}', texto)
    if cnpj:
        meta['cnpj_fornecedor'] = cnpj.group()

    # Número da NF
    nf = re.search(r'(?:NF-?e?|Nota\s+Fiscal)\s*[nN]?[oO°]?\s*(\d{6,})', texto, re.I)
    if nf:
        meta['numero_nf'] = nf.group(1)

    # Data de emissão
    data = re.search(r'(\d{2}/\d{2}/\d{4})', texto)
    if data:
        try:
            d = datetime.strptime(data.group(1), '%d/%m/%Y')
            meta['data_emissao'] = d.strftime('%Y-%m-%d')
        except:
            pass

    # Valor total
    totais = re.findall(r'(?:TOTAL|Total|Valor\s+Total)[^\d]*(\d{1,3}(?:\.\d{3})*,\d{2})', texto, re.I)
    if totais:
        try:
            meta['valor_total_nf'] = float(totais[-1].replace('.', '').replace(',', '.'))
        except:
            pass

    # Nome do fornecedor (primeira linha não vazia)
    linhas = [l.strip() for l in texto.split('\n') if len(l.strip()) > 5]
    if linhas:
        meta['fornecedor'] = linhas[0][:120]

    return meta


# ══════════════════════════════════════════════════════════════════════════════
# ENDPOINTS
# ══════════════════════════════════════════════════════════════════════════════

@app.route('/health', methods=['GET'])
def health():
    """Verifica se o serviço está rodando."""
    return jsonify({
        "status": "ok",
        "servico": "Mecânica DBO OCR",
        "tesseract": str(pytesseract.get_tesseract_version()),
        "idiomas": IDIOMAS
    })


@app.route('/ocr/processar', methods=['POST'])
def processar_nf():
    """
    Recebe imagem da NF e retorna itens extraídos.

    Body (JSON):
    {
        "imagem": "<base64_da_imagem>",
        "formato": "jpg" | "png"  (opcional, default: jpg)
    }

    Ou multipart/form-data com campo "file"
    """
    try:
        img_array = None

        # ── Recebe imagem ─────────────────────────────────────────────────────
        if request.is_json:
            dados = request.get_json()
            imagem_b64 = dados.get('imagem', '')
            if not imagem_b64:
                return jsonify({"erro": "Campo 'imagem' obrigatório"}), 400

            # Decodifica base64
            try:
                img_bytes = base64.b64decode(imagem_b64, validate=False)
            except Exception:
                return jsonify({"erro": "Imagem em base64 inválida"}), 400
            img_array = np.frombuffer(img_bytes, dtype=np.uint8)
            img_array = cv2.imdecode(img_array, cv2.IMREAD_COLOR)

        elif 'file' in request.files:
            arquivo = request.files['file']
            img_bytes = arquivo.read()
            img_array = np.frombuffer(img_bytes, dtype=np.uint8)
            img_array = cv2.imdecode(img_array, cv2.IMREAD_COLOR)

        else:
            return jsonify({"erro": "Envie JSON com 'imagem' em base64 ou multipart com 'file'"}), 400

        if img_array is None:
            return jsonify({"erro": "Não foi possível decodificar a imagem"}), 400

        # ── Processa OCR ──────────────────────────────────────────────────────
        texto_bruto = extrair_texto(img_array)
        itens = parsear_itens(texto_bruto)
        metadados = extrair_metadados(texto_bruto)

        return jsonify({
            "sucesso": True,
            "metadados": metadados,
            "itens": itens,
            "total_itens": len(itens),
            "texto_bruto": texto_bruto,
            "mensagem": f"{len(itens)} item(ns) extraído(s) — revise antes de importar"
        })

    except Exception as e:
        traceback.print_exc()
        return jsonify({
            "sucesso": False,
            "erro": str(e)
        }), 500


@app.route('/ocr/processar-url', methods=['POST'])
def processar_nf_url():
    """
    Alternativa: recebe caminho local da imagem.
    Body: { "caminho": "C:/fotos/nf.jpg" }
    """
    try:
        dados = request.get_json()
        caminho = dados.get('caminho', '')

        if not caminho or not os.path.exists(caminho):
            return jsonify({"erro": f"Arquivo não encontrado: {caminho}"}), 404

        img_array = cv2.imread(caminho)
        if img_array is None:
            return jsonify({"erro": "Não foi possível abrir a imagem"}), 400

        texto_bruto = extrair_texto(img_array)
        itens = parsear_itens(texto_bruto)
        metadados = extrair_metadados(texto_bruto)

        return jsonify({
            "sucesso": True,
            "metadados": metadados,
            "itens": itens,
            "total_itens": len(itens),
            "texto_bruto": texto_bruto,
        })

    except Exception as e:
        traceback.print_exc()
        return jsonify({"sucesso": False, "erro": str(e)}), 500


if __name__ == '__main__':
    from waitress import serve

    # 127.0.0.1 = só esta máquina (o Spring Boot roda no mesmo PC). O endpoint /ocr/processar-url
    # lê arquivos pelo caminho, então NÃO exponha na rede. Para liberar: set OCR_HOST=0.0.0.0
    host = os.environ.get('OCR_HOST', '127.0.0.1')
    port = int(os.environ.get('OCR_PORT', '5000'))

    print("=" * 60)
    print("  Mecânica DBO — Microserviço OCR (Waitress)")
    print(f"  Tesseract: {pytesseract.get_tesseract_version()}")
    print(f"  Endereço: http://{host}:{port}")
    print("  Endpoints:")
    print("    GET  /health")
    print("    POST /ocr/processar       (base64 ou multipart)")
    print("    POST /ocr/processar-url   (caminho local)")
    print("=" * 60)
    serve(app, host=host, port=port, threads=4,
          max_request_body_size=20 * 1024 * 1024)  # imagens de até 20 MB
