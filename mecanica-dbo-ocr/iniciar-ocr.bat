@echo off
rem ============================================================
rem  Mecanica DBO - liga o leitor de notas fiscais (OCR)
rem  De dois cliques neste arquivo. Deixe a janela aberta enquanto
rem  usar a importacao de notas; fechar a janela desliga o OCR.
rem
rem  Primeira vez num PC novo (cria o ambiente e instala as bibliotecas):
rem    py -m venv venv
rem    venv\Scripts\python.exe -m pip install -r requirements.txt
rem  Precisa tambem do Tesseract em C:\Program Files\Tesseract-OCR
rem  (com o idioma portugues).
rem ============================================================
cd /d "%~dp0"

if not exist "venv\Scripts\python.exe" (
  echo Nao achei o ambiente Python em "%~dp0venv".
  echo Siga os passos de "primeira vez" escritos no topo deste arquivo.
  pause
  exit /b 1
)

venv\Scripts\python.exe ocr_service.py
pause
