eval $(opam env --switch=/home/user/goblint/analyzer --set-switch)
cd ./vscode
code --install-extension gobpie-0.0.3.vsix
cd ..
code ~/goblint/example # Replace with your example C code folder (for WSL this must be inside the Linux filesystem i.e. not /mnt/**)
