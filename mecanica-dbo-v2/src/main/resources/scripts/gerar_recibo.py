#!/usr/bin/env python3
"""
Mecânica DBO — Gerador de Recibo PDF
Uso: python3 gerar_recibo.py '<json_da_os>' '<caminho_saida.pdf>'
"""

import sys
import json
from datetime import datetime
from reportlab.lib.pagesizes import A4
from reportlab.lib import colors
from reportlab.lib.units import cm
from reportlab.lib.styles import ParagraphStyle
from reportlab.platypus import (
    SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, HRFlowable
)
from reportlab.lib.enums import TA_CENTER, TA_LEFT, TA_RIGHT

# ── Cores da Mecânica DBO ─────────────────────────────────────────────────────
VERMELHO    = colors.HexColor("#C0392B")
CINZA_ESCURO = colors.HexColor("#2C3E50")
CINZA_MEDIO = colors.HexColor("#7F8C8D")
CINZA_CLARO = colors.HexColor("#ECF0F1")
PRETO       = colors.black
BRANCO      = colors.white

W, H = A4  # 595 x 842 pontos

def fmt_brl(valor):
    """Formata número como R$ 1.234,56"""
    try:
        v = float(valor or 0)
        return f"R$ {v:,.2f}".replace(",", "X").replace(".", ",").replace("X", ".")
    except:
        return "R$ 0,00"

def fmt_data(data_str):
    if not data_str:
        return "—"
    try:
        d = datetime.fromisoformat(str(data_str)[:10])
        return d.strftime("%d/%m/%Y")
    except:
        return str(data_str)[:10]

def gerar_pdf(os_data: dict, caminho_saida: str):
    doc = SimpleDocTemplate(
        caminho_saida,
        pagesize=A4,
        rightMargin=1.5*cm,
        leftMargin=1.5*cm,
        topMargin=1.5*cm,
        bottomMargin=1.5*cm,
    )

    # ── Estilos ───────────────────────────────────────────────────────────────
    s_titulo = ParagraphStyle("titulo",
        fontName="Helvetica-Bold", fontSize=22,
        textColor=CINZA_ESCURO, alignment=TA_CENTER, spaceAfter=2)

    s_subtitulo = ParagraphStyle("subtitulo",
        fontName="Helvetica", fontSize=9,
        textColor=CINZA_MEDIO, alignment=TA_CENTER, spaceAfter=2)

    s_secao = ParagraphStyle("secao",
        fontName="Helvetica-Bold", fontSize=9,
        textColor=BRANCO, alignment=TA_CENTER)

    s_label = ParagraphStyle("label",
        fontName="Helvetica-Bold", fontSize=8, textColor=CINZA_ESCURO)

    s_valor = ParagraphStyle("valor",
        fontName="Helvetica", fontSize=8, textColor=PRETO)

    s_diag = ParagraphStyle("diag",
        fontName="Helvetica", fontSize=8, textColor=PRETO,
        leading=12, spaceAfter=4)

    s_total_label = ParagraphStyle("total_label",
        fontName="Helvetica-Bold", fontSize=9, textColor=CINZA_ESCURO,
        alignment=TA_RIGHT)

    s_total_valor = ParagraphStyle("total_valor",
        fontName="Helvetica-Bold", fontSize=9, textColor=VERMELHO,
        alignment=TA_RIGHT)

    s_rodape = ParagraphStyle("rodape",
        fontName="Helvetica", fontSize=7,
        textColor=CINZA_MEDIO, alignment=TA_CENTER)

    # ── Dados da OS ──────────────────────────────────────────────────────────
    veiculo  = os_data.get("veiculo", {})
    cliente  = veiculo.get("cliente", {}) if veiculo else {}
    pecas    = os_data.get("itensPeca", [])
    servicos = os_data.get("itensServico", [])

    nome_cliente  = cliente.get("nome", "—")
    cpf_cliente   = cliente.get("cpf", "—")
    tel_cliente   = cliente.get("telefone1", "—")
    placa         = veiculo.get("placa", "—")
    marca         = veiculo.get("marca", "—")
    modelo        = veiculo.get("modelo", "—")
    cor           = veiculo.get("cor", "—")
    combustivel   = veiculo.get("combustivel", "—")
    km            = os_data.get("kmEntrada", "—")
    data_entrada  = fmt_data(os_data.get("dataEntrada"))
    data_saida    = fmt_data(os_data.get("dataSaidaReal") or os_data.get("dataSaidaPrevista"))
    reclamacoes   = os_data.get("reclamacoes", "—")
    diagnostico   = os_data.get("diagnostico", "—")
    total_pecas   = fmt_brl(os_data.get("totalPecas", 0))
    total_servicos= fmt_brl(os_data.get("totalServicos", 0))
    total_geral   = fmt_brl(os_data.get("totalGeral", 0))
    os_id         = os_data.get("id", "—")

    elementos = []

    # ── CABEÇALHO ─────────────────────────────────────────────────────────────
    cab_data = [
        [
            Paragraph("MECÂNICA DBO", s_titulo),
            Paragraph(
                "Quadra 204, Conjunto 05, Casa 01\n"
                "Recanto das Emas — CEP 72610-405\n"
                "Fone: (61) 98205-8689",
                s_subtitulo
            ),
        ]
    ]
    cab_table = Table(cab_data, colWidths=[9*cm, 7.5*cm])
    cab_table.setStyle(TableStyle([
        ("VALIGN",      (0,0), (-1,-1), "MIDDLE"),
        ("ALIGN",       (0,0), (0,0),   "LEFT"),
        ("ALIGN",       (1,0), (1,0),   "RIGHT"),
        ("BOTTOMPADDING",(0,0),(-1,-1), 4),
    ]))
    elementos.append(cab_table)
    elementos.append(HRFlowable(width="100%", thickness=2, color=VERMELHO, spaceAfter=6))

    # ── TÍTULO DO RECIBO ──────────────────────────────────────────────────────
    elementos.append(Paragraph(f"RECIBO DE SERVIÇO — OS #{os_id}", ParagraphStyle(
        "rec", fontName="Helvetica-Bold", fontSize=11,
        textColor=VERMELHO, alignment=TA_CENTER, spaceAfter=8
    )))

    # ── SEÇÃO: DADOS DO PROPRIETÁRIO ─────────────────────────────────────────
    def celula_secao(txt):
        return Paragraph(txt, s_secao)

    def linha_dado(label, valor):
        return [Paragraph(label, s_label), Paragraph(str(valor), s_valor)]

    # Cabeçalho seção
    sec1 = Table([[celula_secao("DADOS DO PROPRIETÁRIO")]], colWidths=[16.5*cm])
    sec1.setStyle(TableStyle([
        ("BACKGROUND",    (0,0),(-1,-1), CINZA_ESCURO),
        ("TOPPADDING",    (0,0),(-1,-1), 4),
        ("BOTTOMPADDING", (0,0),(-1,-1), 4),
    ]))
    elementos.append(sec1)

    prop_data = [
        [Paragraph("NOME:", s_label), Paragraph(nome_cliente, s_valor),
         Paragraph("CPF:", s_label),  Paragraph(cpf_cliente, s_valor)],
        [Paragraph("TELEFONE:", s_label), Paragraph(tel_cliente, s_valor),
         Paragraph("DATA:", s_label),
         Paragraph(f"{data_entrada} a {data_saida}", s_valor)],
    ]
    prop_table = Table(prop_data, colWidths=[2.5*cm, 5.5*cm, 2*cm, 6.5*cm])
    prop_table.setStyle(TableStyle([
        ("GRID",          (0,0),(-1,-1), 0.3, colors.HexColor("#BDC3C7")),
        ("BACKGROUND",    (0,0),(0,-1), CINZA_CLARO),
        ("BACKGROUND",    (2,0),(2,-1), CINZA_CLARO),
        ("TOPPADDING",    (0,0),(-1,-1), 4),
        ("BOTTOMPADDING", (0,0),(-1,-1), 4),
        ("LEFTPADDING",   (0,0),(-1,-1), 5),
    ]))
    elementos.append(prop_table)
    elementos.append(Spacer(1, 4))

    # ── SEÇÃO: DADOS DO VEÍCULO ───────────────────────────────────────────────
    sec2 = Table([[celula_secao("DADOS DO VEÍCULO")]], colWidths=[16.5*cm])
    sec2.setStyle(TableStyle([
        ("BACKGROUND",    (0,0),(-1,-1), CINZA_ESCURO),
        ("TOPPADDING",    (0,0),(-1,-1), 4),
        ("BOTTOMPADDING", (0,0),(-1,-1), 4),
    ]))
    elementos.append(sec2)

    veic_data = [
        [Paragraph("MODELO:", s_label), Paragraph(modelo, s_valor),
         Paragraph("KM:", s_label),     Paragraph(str(km), s_valor),
         Paragraph("MARCA:", s_label),  Paragraph(marca, s_valor),
         Paragraph("PLACA:", s_label),  Paragraph(placa, s_valor)],
        [Paragraph("COR:", s_label),    Paragraph(cor, s_valor),
         Paragraph("COMBUSTÍVEL:", s_label), Paragraph(combustivel, s_valor),
         Paragraph("", s_label),        Paragraph("", s_valor),
         Paragraph("", s_label),        Paragraph("", s_valor)],
    ]
    veic_table = Table(veic_data, colWidths=[2*cm, 3.5*cm, 1.2*cm, 1.8*cm, 2*cm, 2.5*cm, 1.5*cm, 2*cm])
    veic_table.setStyle(TableStyle([
        ("GRID",          (0,0),(-1,-1), 0.3, colors.HexColor("#BDC3C7")),
        ("BACKGROUND",    (0,0),(0,-1), CINZA_CLARO),
        ("BACKGROUND",    (2,0),(2,-1), CINZA_CLARO),
        ("BACKGROUND",    (4,0),(4,-1), CINZA_CLARO),
        ("BACKGROUND",    (6,0),(6,-1), CINZA_CLARO),
        ("TOPPADDING",    (0,0),(-1,-1), 4),
        ("BOTTOMPADDING", (0,0),(-1,-1), 4),
        ("LEFTPADDING",   (0,0),(-1,-1), 4),
    ]))
    elementos.append(veic_table)
    elementos.append(Spacer(1, 4))

    # ── SEÇÃO: RECLAMAÇÕES ────────────────────────────────────────────────────
    sec3 = Table([[celula_secao("RECLAMAÇÕES DO CLIENTE")]], colWidths=[16.5*cm])
    sec3.setStyle(TableStyle([
        ("BACKGROUND",    (0,0),(-1,-1), VERMELHO),
        ("TOPPADDING",    (0,0),(-1,-1), 4),
        ("BOTTOMPADDING", (0,0),(-1,-1), 4),
    ]))
    elementos.append(sec3)
    elementos.append(Table(
        [[Paragraph(reclamacoes or "—", s_diag)]],
        colWidths=[16.5*cm],
        style=[
            ("GRID",          (0,0),(-1,-1), 0.3, colors.HexColor("#BDC3C7")),
            ("TOPPADDING",    (0,0),(-1,-1), 5),
            ("BOTTOMPADDING", (0,0),(-1,-1), 5),
            ("LEFTPADDING",   (0,0),(-1,-1), 6),
        ]
    ))
    elementos.append(Spacer(1, 4))

    # ── SEÇÃO: PEÇAS E SERVIÇOS LADO A LADO ──────────────────────────────────
    # Cabeçalhos
    h_peca = Table(
        [[Paragraph("DADOS DO PRODUTO (PEÇAS)", s_secao)]],
        colWidths=[8*cm]
    )
    h_peca.setStyle(TableStyle([
        ("BACKGROUND", (0,0),(-1,-1), CINZA_ESCURO),
        ("TOPPADDING", (0,0),(-1,-1), 4),
        ("BOTTOMPADDING",(0,0),(-1,-1), 4),
    ]))

    h_serv = Table(
        [[Paragraph("DADOS DO SERVIÇO (MÃO DE OBRA)", s_secao)]],
        colWidths=[8*cm]
    )
    h_serv.setStyle(TableStyle([
        ("BACKGROUND", (0,0),(-1,-1), CINZA_ESCURO),
        ("TOPPADDING", (0,0),(-1,-1), 4),
        ("BOTTOMPADDING",(0,0),(-1,-1), 4),
    ]))

    cab_duplo = Table([[h_peca, Paragraph("", s_label), h_serv]],
                      colWidths=[8*cm, 0.5*cm, 8*cm])
    elementos.append(cab_duplo)

    # Linhas de itens
    s_th = ParagraphStyle("th", fontName="Helvetica-Bold", fontSize=7,
                          textColor=BRANCO, alignment=TA_CENTER)
    s_td = ParagraphStyle("td", fontName="Helvetica", fontSize=7,
                          textColor=PRETO)
    s_td_r = ParagraphStyle("td_r", fontName="Helvetica", fontSize=7,
                             textColor=PRETO, alignment=TA_RIGHT)

    def build_pecas():
        rows = [[
            Paragraph("#", s_th),
            Paragraph("DESCRIÇÃO", s_th),
            Paragraph("VALOR", s_th),
        ]]
        for i, p in enumerate(pecas, 1):
            bg = CINZA_CLARO if i % 2 == 0 else BRANCO
            pago = " *" if p.get("pagoPeloCliente") else ""
            rows.append([
                Paragraph(str(i), s_td),
                Paragraph(str(p.get("descricao","")) + pago, s_td),
                Paragraph(fmt_brl(p.get("valorTotal", p.get("valorUnitario",0))), s_td_r),
            ])
        # Preenche até 10 linhas mínimas
        while len(rows) < 11:
            rows.append([Paragraph("", s_td), Paragraph("", s_td), Paragraph("", s_td)])

        t = Table(rows, colWidths=[0.6*cm, 5.4*cm, 2*cm])
        t.setStyle(TableStyle([
            ("BACKGROUND",    (0,0),(-1,0), CINZA_ESCURO),
            ("GRID",          (0,0),(-1,-1), 0.3, colors.HexColor("#BDC3C7")),
            ("TOPPADDING",    (0,0),(-1,-1), 3),
            ("BOTTOMPADDING", (0,0),(-1,-1), 3),
            ("LEFTPADDING",   (0,0),(-1,-1), 3),
        ]))
        return t

    def build_servicos():
        rows = [[
            Paragraph("#", s_th),
            Paragraph("DESCRIÇÃO", s_th),
            Paragraph("VALOR", s_th),
        ]]
        for i, s in enumerate(servicos, 1):
            rows.append([
                Paragraph(str(i), s_td),
                Paragraph(str(s.get("descricao","")), s_td),
                Paragraph(fmt_brl(s.get("valor",0)), s_td_r),
            ])
        while len(rows) < 11:
            rows.append([Paragraph("", s_td), Paragraph("", s_td), Paragraph("", s_td)])

        t = Table(rows, colWidths=[0.6*cm, 5.4*cm, 2*cm])
        t.setStyle(TableStyle([
            ("BACKGROUND",    (0,0),(-1,0), CINZA_ESCURO),
            ("GRID",          (0,0),(-1,-1), 0.3, colors.HexColor("#BDC3C7")),
            ("TOPPADDING",    (0,0),(-1,-1), 3),
            ("BOTTOMPADDING", (0,0),(-1,-1), 3),
            ("LEFTPADDING",   (0,0),(-1,-1), 3),
        ]))
        return t

    itens_duplo = Table(
        [[build_pecas(), Paragraph("", s_td), build_servicos()]],
        colWidths=[8*cm, 0.5*cm, 8*cm]
    )
    itens_duplo.setStyle(TableStyle([("VALIGN",(0,0),(-1,-1),"TOP")]))
    elementos.append(itens_duplo)
    elementos.append(Spacer(1, 4))

    # ── TOTAIS ────────────────────────────────────────────────────────────────
    totais_data = [
        [Paragraph("TOTAL PEÇAS:", s_total_label),
         Paragraph(total_pecas,    s_total_valor),
         Paragraph("TOTAL SERVIÇOS:", s_total_label),
         Paragraph(total_servicos,    s_total_valor)],
        [Paragraph("", s_label), Paragraph("", s_label),
         Paragraph("TOTAL GERAL:", ParagraphStyle("tg", fontName="Helvetica-Bold",
             fontSize=11, textColor=VERMELHO, alignment=TA_RIGHT)),
         Paragraph(total_geral, ParagraphStyle("tv", fontName="Helvetica-Bold",
             fontSize=11, textColor=VERMELHO, alignment=TA_RIGHT))],
    ]
    totais_table = Table(totais_data, colWidths=[4*cm, 3.5*cm, 5*cm, 4*cm])
    totais_table.setStyle(TableStyle([
        ("ALIGN",         (0,0),(-1,-1), "RIGHT"),
        ("TOPPADDING",    (0,0),(-1,-1), 3),
        ("BOTTOMPADDING", (0,0),(-1,-1), 3),
        ("LINEABOVE",     (0,0),(-1,0), 1, CINZA_ESCURO),
        ("LINEABOVE",     (0,1),(-1,1), 0.5, CINZA_CLARO),
        ("LINEBELOW",     (0,-1),(-1,-1), 1.5, VERMELHO),
    ]))
    elementos.append(totais_table)
    elementos.append(Spacer(1, 6))

    # ── SEÇÃO: DIAGNÓSTICO ────────────────────────────────────────────────────
    sec4 = Table([[celula_secao("DIAGNÓSTICO E OBSERVAÇÕES DO MECÂNICO")]], colWidths=[16.5*cm])
    sec4.setStyle(TableStyle([
        ("BACKGROUND",    (0,0),(-1,-1), CINZA_ESCURO),
        ("TOPPADDING",    (0,0),(-1,-1), 4),
        ("BOTTOMPADDING", (0,0),(-1,-1), 4),
    ]))
    elementos.append(sec4)
    elementos.append(Table(
        [[Paragraph(diagnostico or "—", s_diag)]],
        colWidths=[16.5*cm],
        style=[
            ("GRID",          (0,0),(-1,-1), 0.3, colors.HexColor("#BDC3C7")),
            ("TOPPADDING",    (0,0),(-1,-1), 6),
            ("BOTTOMPADDING", (0,0),(-1,-1), 6),
            ("LEFTPADDING",   (0,0),(-1,-1), 6),
        ]
    ))
    elementos.append(Spacer(1, 12))

    # ── ASSINATURAS ───────────────────────────────────────────────────────────
    ass_data = [[
        Paragraph("_______________________________\nAssinatura do Cliente", ParagraphStyle(
            "ass", fontName="Helvetica", fontSize=8,
            textColor=CINZA_ESCURO, alignment=TA_CENTER)),
        Paragraph("", s_label),
        Paragraph("_______________________________\nAssinatura do Mecânico", ParagraphStyle(
            "ass2", fontName="Helvetica", fontSize=8,
            textColor=CINZA_ESCURO, alignment=TA_CENTER)),
    ]]
    ass_table = Table(ass_data, colWidths=[7*cm, 2.5*cm, 7*cm])
    elementos.append(ass_table)
    elementos.append(Spacer(1, 10))

    # ── RODAPÉ ────────────────────────────────────────────────────────────────
    elementos.append(HRFlowable(width="100%", thickness=1, color=CINZA_MEDIO, spaceAfter=4))
    elementos.append(Paragraph(
        f"Mecânica DBO  •  Recanto das Emas, Brasília/DF  •  (61) 98205-8689  •  "
        f"Documento gerado em {datetime.now().strftime('%d/%m/%Y às %H:%M')}  •  OS #{os_id}",
        s_rodape
    ))
    if pecas:
        elementos.append(Paragraph("* Peça paga/trazida pelo cliente", s_rodape))

    # ── BUILD ─────────────────────────────────────────────────────────────────
    doc.build(elementos)
    print(f"PDF gerado: {caminho_saida}")


if __name__ == "__main__":
    if len(sys.argv) < 3:
        # Modo teste com dados da Tatiana Silva
        os_teste = {
            "id": 1,
            "dataEntrada": "2026-08-16",
            "dataSaidaReal": "2026-08-20",
            "kmEntrada": 87500,
            "status": "ENTREGUE",
            "reclamacoes": "1- Direção hidráulica com barulho e óleo vazando\n2- Barulho na roda\n3- Barulho na suspensão",
            "diagnostico": "1- Diagnóstico: Direção hidráulica vazando na caixa reduzindo o nível óleo gerando cavitação na bomba hidráulica.\n2- Diagnóstico: Rolamento da roda traseira direita com fadiga e perda de lubrificação.\n3- Diagnóstico: Suspensão com defeito na bucha da barra estabilizadora e na bieleta por conta de contaminação de óleo hidráulico da caixa de direção.",
            "totalPecas": 394.57,
            "totalServicos": 810.00,
            "totalGeral": 1204.57,
            "veiculo": {
                "placa": "JEH-3030", "marca": "Renault",
                "modelo": "Sandero 1.6 8v", "cor": "PRETA",
                "combustivel": "GASOLINA",
                "cliente": {
                    "nome": "Tatiana Silva",
                    "cpf": "***.***.**-**",
                    "telefone1": "(61) 99209-4960"
                }
            },
            "itensPeca": [
                {"descricao": "Manutenção de terceiros, direção hidráulica", "valorTotal": 0, "pagoPeloCliente": False},
                {"descricao": "Peças pagas pelo cliente", "valorTotal": 800.00, "pagoPeloCliente": True},
                {"descricao": "Óleo", "valorTotal": 0, "pagoPeloCliente": False},
                {"descricao": "Filtro de óleo", "valorTotal": 0, "pagoPeloCliente": False},
                {"descricao": "Rolamento da roda traseira direita", "valorTotal": 0, "pagoPeloCliente": False},
                {"descricao": "Peças pagas pelo cliente", "valorTotal": 255.83, "pagoPeloCliente": True},
                {"descricao": "Pastinha de freio", "valorTotal": 74.28, "pagoPeloCliente": False},
                {"descricao": "Bieleta", "valorTotal": 46.55, "pagoPeloCliente": False},
                {"descricao": "Bucha estabilizadora", "valorTotal": 15.23, "pagoPeloCliente": False},
                {"descricao": "Crédito mecânico", "valorTotal": -258.51, "pagoPeloCliente": False},
            ],
            "itensServico": [
                {"descricao": "Troca de rolamento", "valor": 200.00},
                {"descricao": "Revisão", "valor": 200.00},
                {"descricao": "Suspensão", "valor": 150.00},
                {"descricao": "Alinhamento e cambagem", "valor": 260.00},
            ]
        }
        gerar_pdf(os_teste, "/home/claude/recibo_teste.pdf")
    else:
        os_data = json.loads(sys.argv[1])
        gerar_pdf(os_data, sys.argv[2])
