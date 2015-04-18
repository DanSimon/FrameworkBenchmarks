## put sed here later

export NVM_HOME=${IROOT}/nvm

# Used to avoid nvm's return 2 error.
# Sourcing this functions if 0 is returned.
source $NVM_HOME/nvm.sh || 0
nvm install 0.12.2
nvm use 0.12.2

# update npm before app init
npm install -g npm
npm install -g sails

# run app
npm install
sails lift --port 8080 &
