#!/bin/bash

SDK_DIR="$HOME/Library/Android/sdk"
CMDLINE_TOOLS_URL="https://dl.google.com/android/repository/commandlinetools-mac-8512546_latest.zip"
CMDLINE_TOOLS_ZIP="commandlinetools.zip"

# Create directories
mkdir -p "$SDK_DIR"
mkdir -p "$SDK_DIR/cmdline-tools"

# Download command line tools
echo "Downloading Android command line tools..."
curl -L "$CMDLINE_TOOLS_URL" -o "$CMDLINE_TOOLS_ZIP"

# Extract command line tools
echo "Extracting command line tools..."
unzip -q "$CMDLINE_TOOLS_ZIP"
mv cmdline-tools "$SDK_DIR/cmdline-tools/latest"
rm "$CMDLINE_TOOLS_ZIP"

# Set environment variables
export ANDROID_HOME="$SDK_DIR"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"

# Accept licenses
echo "Accepting licenses..."
yes | sdkmanager --licenses

# Install required SDK components
echo "Installing SDK components..."
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"

echo "Android SDK setup completed at $SDK_DIR"
echo "Please add the following to your .bash_profile or .zshrc:"
echo 'export ANDROID_HOME="$HOME/Library/Android/sdk"'
echo 'export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"' 