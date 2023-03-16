# Load environment variables from .env
export $(grep -v '^#' .env | xargs)

code --install-extension gobpie-0.0.3.vsix
code $GOBPIE_TEST_PROJECT
