@echo off
setlocal enabledelayedexpansion

echo ============================================
echo   Build Calculadora Android (sem Gradle)
echo ============================================

:: ================= CONFIGURAÇÕES =================
set SDK_PATH=C:\Users\\AppData\Local\Android\Sdk
set BUILD_TOOLS=%SDK_PATH%\build-tools\34.0.0
set PLATFORM=%SDK_PATH%\platforms\android-34
set AAPT2=%BUILD_TOOLS%\aapt2.exe
set D8=%BUILD_TOOLS%\d8.bat
set ZIPALIGN=%BUILD_TOOLS%\zipalign.exe
set APKSIGNER_JAR=%BUILD_TOOLS%\lib\apksigner.jar

set APP_NAME=CalculadoraBonita
set OUT_DIR=bin
set OBJ_DIR=obj
set SRC_DIR=src
set MANIFEST=AndroidManifest.xml

:: ================= VERIFICAÇÃO DAS FERRAMENTAS =================
echo Verificando ferramentas do SDK...
for %%F in ("%AAPT2%" "%D8%" "%ZIPALIGN%" "%APKSIGNER_JAR%") do (
    if not exist %%F (
        echo ERRO: %%F nao encontrado.
        pause
        exit /b 1
    )
)
echo Todas as ferramentas encontradas.

:: Cria pastas
if not exist "%OUT_DIR%" mkdir "%OUT_DIR%"
if not exist "%OBJ_DIR%" mkdir "%OBJ_DIR%"

:: ================= LIMPEZA =================
echo [1/8] Limpando builds anteriores...
if exist "%OUT_DIR%\*.apk" del "%OUT_DIR%\*.apk"
if exist "%OUT_DIR%\*.dex" del "%OUT_DIR%\*.dex"
if exist "%OBJ_DIR%\*.class" del /s /q "%OBJ_DIR%\*.class" >nul 2>&1

:: ================= COMPILAÇÃO JAVA =================
echo [2/8] Compilando fontes Java...
dir /s /b "%SRC_DIR%\*.java" > sources.txt
set CLASSPATH=%PLATFORM%\android.jar
javac -cp "%CLASSPATH%" -d "%OBJ_DIR%" @sources.txt
if %errorlevel% neq 0 (
    echo ERRO: Falha na compilacao Java.
    del sources.txt
    pause
    exit /b 1
)
del sources.txt
echo Compilacao Java concluida.

:: ================= CONVERSÃO DEX (D8) =================
echo [3/8] Convertendo bytecode para DEX...
call "%D8%" --lib "%PLATFORM%\android.jar" --output "%OUT_DIR%" %OBJ_DIR%\com\exemplo\calculadora\*.class
if %errorlevel% neq 0 (
    echo ERRO: Falha na conversao DEX.
    pause
    exit /b 1
)
:: Verifica tamanho do classes.dex
for %%A in ("%OUT_DIR%\classes.dex") do set DEX_SIZE=%%~zA
echo DEX gerado com %DEX_SIZE% bytes.
if %DEX_SIZE% LSS 500 (
    echo ERRO: classes.dex muito pequeno, provavelmente corrompido.
    pause
    exit /b 1
)
echo Conversao DEX concluida.

:: ================= GERAÇÃO DO APK BASE (AAPT2 LINK sem dex) =================
echo [4/8] Gerando APK base com aapt2...
"%AAPT2%" link -o "%OUT_DIR%\%APP_NAME%.unaligned.apk" ^
    -I "%PLATFORM%\android.jar" ^
    --manifest "%MANIFEST%" ^
    --auto-add-overlay
if %errorlevel% neq 0 (
    echo ERRO: Falha no link do aapt2.
    pause
    exit /b 1
)
echo APK base gerado.

:: ================= INSERIR DEX NO APK (USANDO jar DO JDK) =================
echo [5/8] Inserindo classes.dex no APK...
cd "%OUT_DIR%"
:: Copia o APK original para outro arquivo
copy /y "%APP_NAME%.unaligned.apk" "%APP_NAME%.withdex.apk" >nul
:: Adiciona o classes.dex ao APK (substitui se já existir)
jar uf "%APP_NAME%.withdex.apk" classes.dex
if %errorlevel% neq 0 (
    echo ERRO: Falha ao adicionar DEX ao APK.
    cd ..
    pause
    exit /b 1
)
:: Substitui o APK base pelo que contém o DEX
move /y "%APP_NAME%.withdex.apk" "%APP_NAME%.unaligned.apk" >nul
cd ..
echo DEX inserido com sucesso.

:: ================= ALINHAMENTO =================
echo [6/8] Alinhando APK...
"%ZIPALIGN%" -p -f 4 "%OUT_DIR%\%APP_NAME%.unaligned.apk" "%OUT_DIR%\%APP_NAME%.aligned.apk"
if %errorlevel% neq 0 (
    echo ERRO: Falha no zipalign.
    pause
    exit /b 1
)
del "%OUT_DIR%\%APP_NAME%.unaligned.apk"
echo Alinhamento concluido.

:: ================= KEYSTORE =================
echo [7/8] Preparando keystore de debug...
if exist "debug.keystore" goto :assinar
if exist "%USERPROFILE%\.android\debug.keystore" (
    copy "%USERPROFILE%\.android\debug.keystore" "debug.keystore" >nul
    echo Keystore copiada do perfil Android.
    goto :assinar
)
where keytool >nul 2>&1
if %errorlevel% equ 0 (
    echo Gerando nova keystore com keytool...
    keytool -genkey -v -keystore debug.keystore -storepass android -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug, O=Android, C=US"
    if %errorlevel% equ 0 (
        echo Keystore de debug gerada com sucesso.
        goto :assinar
    ) else (
        echo ERRO: Falha ao gerar keystore com keytool.
        pause
        exit /b 1
    )
)
echo ERRO: Nao foi possivel obter uma keystore de debug.
echo Solucoes:
echo 1. Instale o JDK completo e adicione a pasta bin ao PATH.
echo 2. Copie manualmente um arquivo debug.keystore para a raiz do projeto.
pause
exit /b 1

:assinar
echo [8/8] Assinando APK...
set APK_ALIGNED=%OUT_DIR%\%APP_NAME%.aligned.apk
set APK_SIGNED=%OUT_DIR%\%APP_NAME%.apk

java -jar "%APKSIGNER_JAR%" sign --ks debug.keystore --ks-pass pass:android --ks-key-alias androiddebugkey --key-pass pass:android --out "%APK_SIGNED%" "%APK_ALIGNED%"
if %errorlevel% neq 0 (
    echo ERRO: Falha na assinatura do APK.
    del "%APK_ALIGNED%" 2>nul
    pause
    exit /b 1
)
del "%APK_ALIGNED%"
echo Assinatura concluida.

echo.
echo ============================================
echo   Build concluido com sucesso!
echo   APK gerado em: %APK_SIGNED%
echo ============================================
pause