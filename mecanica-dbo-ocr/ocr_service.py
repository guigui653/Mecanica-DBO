#!/usr/bin/env python3
"""
Mecânica DBO — Microserviço OCR v2
Lê notas fiscais térmicas via imagem e extrai itens automaticamente
Porta: 5000

MELHORIAS v2:
- Detecção e recorte automático da região do papel (elimina fundo/ruído)
- Pré-processamento com CLAHE + Otsu (melhor para papel térmico desbotado)
- Parser mais tolerante a erros comuns de OCR
"""

import os
import re
import json
import base64
import traceback
from datetime import datetime

import cv2
import numpy as np
import pytesseract
from PIL import Image
from flask import Flask, request, jsonify

try:
    from pdf2image import convert_from_path
    PDF_SUPORTADO = True
except ImportError:
    PDF_SUPORTADO = False

# ── Configuração do Tesseract (Windows) ───────────────────────────────────────
CAMINHO_TESSERACT = r"C:\Program Files\Tesseract-OCR\tesseract.exe"
if os.path.exists(CAMINHO_TESSERACT):
    pytesseract.pytesseract.tesseract_cmd = CAMINHO_TESSERACT

app = Flask(__name__)


# ══════════════════════════════════════════════════════════════════════════════
# ETAPA 1 — Detecção e recorte automático do papel
# ══════════════════════════════════════════════════════════════════════════════
def recortar_papel(img: np.ndarray) -> np.ndarray:
    """
    Detecta a maior região clara da imagem (o papel/recibo) e recorta,
    eliminando fundo escuro (mesa, teclado, mão, etc).
    Se não conseguir detectar com confiança, retorna a imagem original.
    """
    h, w = img.shape[:2]
    gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)

    _, mask = cv2.threshold(gray, 140, 255, cv2.THRESH_BINARY)
    kernel = np.ones((25, 25), np.uint8)
    mask = cv2.morphologyEx(mask, cv2.MORPH_CLOSE, kernel)
    mask = cv2.morphologyEx(mask, cv2.MORPH_OPEN, kernel)

    contornos, _ = cv2.findContours(mask, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
    if not contornos:
        return img

    maior = max(contornos, key=cv2.contourArea)
    area_maior = cv2.contourArea(maior)

    # Só usa o recorte se a região detectada for significativa (>15% da imagem)
    if area_maior < (w * h * 0.15):
        return img

    x, y, cw, ch = cv2.boundingRect(maior)
    margem = 10
    x1, y1 = max(0, x - margem), max(0, y - margem)
    x2, y2 = min(w, x + cw + margem), min(h, y + ch + margem)

    return img[y1:y2, x1:x2]


# ══════════════════════════════════════════════════════════════════════════════
# ETAPA 2 — Pré-processamento para OCR
# ══════════════════════════════════════════════════════════════════════════════
def preprocessar_imagem(img: np.ndarray) -> np.ndarray:
    """Prepara a imagem (já recortada) para leitura do Tesseract."""
    gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY) if len(img.shape) == 3 else img

    h, w = gray.shape
    if w < 1000:
        scale = 1000 / w
        gray = cv2.resize(gray, None, fx=scale, fy=scale, interpolation=cv2.INTER_CUBIC)

    clahe = cv2.createCLAHE(clipLimit=3.0, tileGridSize=(8, 8))
    gray = clahe.apply(gray)

    _, binario = cv2.threshold(gray, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)

    return binario


def extrair_texto(img: np.ndarray) -> str:
    """Pipeline completo: recorta papel → preprocessa → OCR."""
    img_recortada = recortar_papel(img)
    img_proc = preprocessar_imagem(img_recortada)
    pil_img = Image.fromarray(img_proc)

    config = "--psm 6 --oem 3 -l por"
    return pytesseract.image_to_string(pil_img, config=config)


# ══════════════════════════════════════════════════════════════════════════════
# ETAPA 3 — Parser de itens (tolerante a ruído de OCR)
# ══════════════════════════════════════════════════════════════════════════════
def _to_float(valor_str: str) -> float:
    """Converte '1.403,60' ou '461,10' para float."""
    limpo = valor_str.replace('.', '').replace(',', '.')
    return float(limpo)


def parsear_itens(texto: str) -> list:
    """
    Extrai itens de notas fiscais no padrão:
        CÓDIGO DESCRIÇÃO
        QTDE UN x VALOR_UNIT   VALOR_TOTAL

    Também cobre o padrão genérico: descrição ... valor no fim da linha.
    """
    itens = []
    linhas = [l.strip() for l in texto.split('\n') if l.strip()]

    ignorar_termos = [
        'cnpj', 'cpf', 'nota fiscal', 'nfc-e', 'nf-e', 'protocolo',
        'autorizacao', 'autorização', 'forma de pagamento', 'consulte',
        'chave de acesso', 'fazenda', 'consumidor', 'procon', 'troca',
        'garantia', 'devolucao', 'vendedor', 'tributos', 'total de itens',
        'valor total', 'quadra', 'brasilia', 'nao incidencia',
    ]

    # Padrão: descrição termina a linha, valores em linha própria/seguinte
    # Ex: "002 63666 AMORTECEDOR DIANTEIRO LD DESPECTIUM"
    #     "1        UN    461,10          461,10"
    padrao_item_cabecalho = re.compile(
        r'^\d{2,3}\s*\d{4,6}\s+(.+)$'  # código + código produto + descrição
    )
    padrao_valores = re.compile(
        r'(\d+)\s*[.,]?\s*'                          # quantidade
        r'UN\.?\s*'                                   # unidade
        r'[Xx*]?\s*'
        r'(\d{1,3}(?:\.\d{3})*,\d{2})\s+'            # valor unitário
        r'(\d{1,3}(?:\.\d{3})*,\d{2})'               # valor total
    )

    i = 0
    while i < len(linhas):
        linha = linhas[i]
        linha_lower = linha.lower()

        if any(termo in linha_lower for termo in ignorar_termos):
            i += 1
            continue

        # Tenta achar cabeçalho de item (código + descrição)
        m_cab = padrao_item_cabecalho.match(linha)
        if m_cab and len(m_cab.group(1)) > 5:
            descricao = m_cab.group(1).strip()

            # Olha a próxima linha (ou a mesma) por quantidade/valores
            texto_busca = linha
            if i + 1 < len(linhas):
                texto_busca += ' ' + linhas[i + 1]

            m_val = padrao_valores.search(texto_busca)
            if m_val:
                qtd = int(m_val.group(1))
                v_unit = _to_float(m_val.group(2))
                v_total = _to_float(m_val.group(3))

                itens.append({
                    "descricao": descricao[:200],
                    "quantidade": qtd,
                    "valor_unitario": v_unit,
                    "valor_total": v_total,
                    "pago_pelo_cliente": False,
                    "origem": "ocr"
                })
                i += 2
                continue

        # Fallback: linha com descrição + valor monetário no final
        valores_na_linha = re.findall(r'\d{1,3}(?:\.\d{3})*,\d{2}', linha)
        if valores_na_linha and len(linha) > 12:
            descricao = re.sub(r'\d{1,3}(?:\.\d{3})*,\d{2}', '', linha).strip()
            descricao = re.sub(r'\s{2,}', ' ', descricao)
            descricao = re.sub(r'^\d+\s*', '', descricao)  # remove código no início

            if len(descricao) >= 5:
                valor = _to_float(valores_na_linha[-1])
                if 0 < valor < 100000:
                    itens.append({
                        "descricao": descricao[:200],
                        "quantidade": 1,
                        "valor_unitario": valor,
                        "valor_total": valor,
                        "pago_pelo_cliente": False,
                        "origem": "ocr"
                    })

        i += 1

    return itens


def extrair_metadados(texto: str) -> dict:
    """Extrai CNPJ, número da NF, data e valor total do texto."""
    meta = {}

    cnpj = re.search(r'\d{2}[.,]?\s?\d{3}[.,]?\s?\d{3}\s?/\s?\d{4}\s?-\s?\d{2}', texto)
    if cnpj:
        meta['cnpj_fornecedor'] = re.sub(r'\s', '', cnpj.group())

    nf = re.search(r'(?:NFC-?e?|NF-?e?)\s*n?[oO°]?\s*(\d{6,})', texto, re.I)
    if nf:
        meta['numero_nf'] = nf.group(1)

    data = re.search(r'(\d{2}/\d{2}/\d{4})', texto)
    if data:
        try:
            d = datetime.strptime(data.group(1), '%d/%m/%Y')
            meta['data_emissao'] = d.strftime('%Y-%m-%d')
        except:
            pass

    totais = re.findall(
        r'(?:valor\s*total|total\s*r\$?)[^\d]{0,10}(\d{1,3}(?:\.\d{3})*,\d{2})',
        texto, re.I
    )
    if totais:
        try:
            meta['valor_total_nf'] = _to_float(totais[-1])
        except:
            pass

    # Nome do fornecedor: procura por linha maiúscula perto do topo
    linhas = [l.strip() for l in texto.split('\n') if len(l.strip()) > 4]
    for l in linhas[:5]:
        letras = re.sub(r'[^A-Za-zÀ-ÿ]', '', l)
        if len(letras) >= 5:
            meta['fornecedor'] = l[:120]
            break

    return meta


# ══════════════════════════════════════════════════════════════════════════════
# ENDPOINTS
# ══════════════════════════════════════════════════════════════════════════════

@app.route('/health', methods=['GET'])
def health():
    return jsonify({
        "status": "ok",
        "servico": "Mecânica DBO OCR",
        "tesseract": str(pytesseract.get_tesseract_version()),
        "pdf_suportado": PDF_SUPORTADO
    })


def _carregar_imagem(caminho: str):
    """Carrega imagem de arquivo, convertendo PDF se necessário."""
    if caminho.lower().endswith('.pdf'):
        if not PDF_SUPORTADO:
            raise RuntimeError("Suporte a PDF não instalado (pip install pdf2image)")
        paginas = convert_from_path(caminho, dpi=250)
        img_pil = paginas[0].convert('RGB')
        return cv2.cvtColor(np.array(img_pil), cv2.COLOR_RGB2BGR)
    else:
        return cv2.imread(caminho)


@app.route('/ocr/processar', methods=['POST'])
def processar_nf():
    """Recebe imagem via base64 ou multipart e retorna itens extraídos."""
    try:
        img_array = None

        if request.is_json:
            dados = request.get_json()
            imagem_b64 = dados.get('imagem', '')
            if not imagem_b64:
                return jsonify({"erro": "Campo 'imagem' obrigatório"}), 400
            img_bytes = base64.b64decode(imagem_b64)
            img_array = cv2.imdecode(np.frombuffer(img_bytes, dtype=np.uint8), cv2.IMREAD_COLOR)

        elif 'file' in request.files:
            arquivo = request.files['file']
            img_bytes = arquivo.read()
            img_array = cv2.imdecode(np.frombuffer(img_bytes, dtype=np.uint8), cv2.IMREAD_COLOR)
        else:
            return jsonify({"erro": "Envie JSON com 'imagem' em base64 ou multipart com 'file'"}), 400

        if img_array is None:
            return jsonify({"erro": "Não foi possível decodificar a imagem"}), 400

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
        return jsonify({"sucesso": False, "erro": str(e)}), 500


@app.route('/ocr/processar-url', methods=['POST'])
def processar_nf_url():
    """Recebe caminho local (imagem ou PDF) e retorna itens extraídos."""
    try:
        dados = request.get_json()
        caminho = dados.get('caminho', '')

        if not caminho or not os.path.exists(caminho):
            return jsonify({"erro": f"Arquivo não encontrado: {caminho}"}), 404

        img_array = _carregar_imagem(caminho)
        if img_array is None:
            return jsonify({"erro": "Não foi possível abrir o arquivo"}), 400

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
    print("=" * 60)
    print("  Mecânica DBO — Microserviço OCR v2")
    print(f"  Tesseract: {pytesseract.get_tesseract_version()}")
    print(f"  Suporte PDF: {'Sim' if PDF_SUPORTADO else 'Não'}")
    print("  Porta: 5000")
    print("  Endpoints:")
    print("    GET  /health")
    print("    POST /ocr/processar       (base64 ou multipart)")
    print("    POST /ocr/processar-url   (caminho local — imagem ou PDF)")
    print("=" * 60)
    app.run(host='0.0.0.0', port=5000, debug=True)