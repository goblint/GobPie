# Load environment variables from .env
export $(grep -v '^#' .env | xargs)

eval $(opam env --switch=$GOBLINT_OPAM_SWITCH --set-switch)
code --install-extension gobpie-0.0.3.vsix
code $GOBPIE_TEST_PROJECT
