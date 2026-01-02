#!/bin/bash

# I.R.I.S Quick Start Script
# This script checks dependencies and launches I.R.I.S in the best available mode

echo "🤖 I.R.I.S - Intelligent Responsive Integrated System"
echo "======================================================"
echo ""

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check Java
echo "1️⃣  Checking Java..."
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -ge 11 ]; then
        echo -e "   ${GREEN}✅ Java $JAVA_VERSION found${NC}"
    else
        echo -e "   ${RED}❌ Java 11+ required (found version $JAVA_VERSION)${NC}"
        exit 1
    fi
else
    echo -e "   ${RED}❌ Java not found. Please install Java 11+${NC}"
    exit 1
fi

# Check Maven
echo ""
echo "2️⃣  Checking Maven..."
if command -v mvn &> /dev/null; then
    echo -e "   ${GREEN}✅ Maven found${NC}"
else
    echo -e "   ${RED}❌ Maven not found. Please install Maven${NC}"
    exit 1
fi

# Check Ollama (for offline AI)
echo ""
echo "3️⃣  Checking Ollama (offline AI)..."
if command -v ollama &> /dev/null; then
    echo -e "   ${GREEN}✅ Ollama found${NC}"
    
    # Check if Ollama is running
    if pgrep -x "ollama" > /dev/null; then
        echo -e "   ${GREEN}✅ Ollama is running${NC}"
    else
        echo -e "   ${YELLOW}⚠️  Ollama not running, starting it...${NC}"
        ollama serve > /dev/null 2>&1 &
        sleep 3
        if pgrep -x "ollama" > /dev/null; then
            echo -e "   ${GREEN}✅ Ollama started${NC}"
        else
            echo -e "   ${YELLOW}⚠️  Could not start Ollama. AI may not work offline.${NC}"
        fi
    fi
    
    # Check for models
    if ollama list 2>/dev/null | grep -qE "(llama|codellama|mistral|phi)"; then
        echo -e "   ${GREEN}✅ AI model available${NC}"
    else
        echo -e "   ${YELLOW}⚠️  No AI model found. Installing llama2...${NC}"
        ollama pull llama2
    fi
else
    echo -e "   ${YELLOW}⚠️  Ollama not found. Online AI mode only.${NC}"
    echo "   Install with: curl -fsSL https://ollama.com/install.sh | sh"
fi

# Build if needed
echo ""
echo "4️⃣  Checking build..."
if [ ! -d "target/classes" ]; then
    echo "   Building project..."
    mvn compile -q
    if [ $? -eq 0 ]; then
        echo -e "   ${GREEN}✅ Build successful${NC}"
    else
        echo -e "   ${RED}❌ Build failed${NC}"
        exit 1
    fi
else
    echo -e "   ${GREEN}✅ Already built${NC}"
fi

# Launch
echo ""
echo "======================================================"
echo -e "${GREEN}✅ All checks passed!${NC}"
echo ""
echo "🚀 Launching I.R.I.S..."
echo ""

# Launch GUI mode
mvn exec:java -Dexec.mainClass="com.jarvis.gui.JarvisGUI" -q
