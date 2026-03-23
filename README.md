# 🚀 fluxtrade-pipeline

## 📈 Overview
fluxtrade is a high-performance **Java 21 / Spring WebFlux** pipeline designed for real-time market data enrichment. It ingests live WebSocket ticks and correlates them with news sentiment to identify market anomalies in microseconds.

## 🛠 Technical Stack
- **Runtime:** Java 21 (Immutable Records, Pattern Matching).
- **Reactive Core:** Project Reactor (Non-blocking, backpressure-aware streams).
- **AI Engine:** Deep Java Library (DJL) running DistilBERT via PyTorch Native.
- **Architecture:** Plugin-based Chain of Responsibility.

## 🧠 The Sentiment Evolution (Tech Pivot)
We systematically evolved our NLP layer to meet HFT requirements:

1. **Deprecated: Stanford CoreNLP**
   - **Reason**: High **Security Risk** and Latency. The library introduced CVE vulnerabilities in transitive dependencies. Architecturally, its 400MB+ neural models caused massive Garbage Collection (GC) spikes.

2. **Deprecated: VADER**
   - **Reason**: Limited **Contextual Accuracy**. While fast, VADER is a rule-based lexicon that fails on linguistic nuances like double negatives (e.g., "not that bad") or complex financial phrasing.

3. **Current: DistilBERT (DJL + PyTorch)**
   - **Reason**: The "Gold Standard." By using **Deep Java Library (DJL)**, we run a Transformer-based model natively on the JVM. This provides human-level accuracy with sub-millisecond inference by offloading matrix math to **C++ PyTorch binaries**.

## ⚡ Key Features
- **Transformer-based Risk**: Native DistilBERT inference isolated on `Schedulers.boundedElastic()` to protect the event loop.
- **Zero-Lock Statistics**: Uses lock-free circular buffers in `RollingStats` for real-time volatility without thread contention.
- **Hardened Dependencies**: Eliminated JitPack and legacy jars to ensure a 100% verified Maven Central dependency tree.

## 🎯 The End Goal
To provide a production-grade foundation for **Automated Risk Mitigation**. The system detects "Black Swan" events by identifying high-volatility price action simultaneous with highly certain negative sentiment, triggering alerts before the broader market reacts.