call mvn package
cd ./vscode
call vsce package
cd ..
wsl -- bash ./install_and_test.sh
