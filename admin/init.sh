#!/bin/bash


sensitive() {
  perl -p -e 's/\$\{([^}]+)\}/defined $ENV{$1} ? $ENV{$1} : $&/eg' < files/credentials-private-repo > ~/.credentials-private-repo
  perl -p -e 's/\$\{([^}]+)\}/defined $ENV{$1} ? $ENV{$1} : $&/eg' < files/credentials-sonatype     > ~/.credentials-sonatype
  perl -p -e 's/\$\{([^}]+)\}/defined $ENV{$1} ? $ENV{$1} : $&/eg' < files/sonatype-curl            > ~/.sonatype-curl
  # perl -p -e 's/\$\{([^}]+)\}/defined $ENV{$1} ? $ENV{$1} : $&/eg' < files/m2-settings.xml          > ~/.m2/settings.xml  -- not needed anymore (used for ide integration?)

  openssl aes-256-cbc -d -K $encrypted_40abf89dbafd_key -iv $encrypted_40abf89dbafd_iv -in files/gpg_subkey.enc | gpg --import
}

# directories needed by sensitive part
# mkdir -p ~/.m2 -- not needed anymore (used for ide integration?)
mkdir -p ~/.ssh

# don't let anything escape from the sensitive part (e.g. leak environment var by echoing to log on failure)
sensitive >/dev/null 2>&1

# just to verify
gpg --list-keys


mkdir -p ~/.sbt/0.13/plugins
cp files/gpg.sbt ~/.sbt/0.13/plugins/

export SBT_CMD=$(which sbt)

$SBT_CMD -v