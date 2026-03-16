# fluxtrade-pipeline

## 🚀 Overview
fluxtrade is a **Java 21 / Spring WebFlux** pipeline built for robust market analysis. It ingests live WebSocket ticks and RESTful news feeds to generate real-time risk scores.

## 🛠 Technical Stack
- **Runtime:** Java 21 (Records, Virtual Threads ready).
- **Reactive Core:** Project Reactor (Non-blocking backpressure-aware streams).
- **NLP Engine:** Stanford CoreNLP for sentiment extraction.
- **Architecture:** Plugin-based Chain of Responsibility.

## 📈 Key Features
- **Zero-Lock Statistics:** Uses `AtomicInteger` and circular buffers in `RollingStats` to calculate volatility without GC pressure.
- **Side-Car State:** A `NewsCache` provides low-latency context to the price stream.
- **Resilient Ingestion:** Automatic backoff retries for WebSocket connections and proactive backpressure dropping to maintain system stability.

## 🎯 The End Goal
To provide a production-grade foundation for **Automated Risk Mitigation**. The system is designed to identify "Black Swan" events by detecting high-volatility price action simultaneous with negative news sentiment, triggering automated sell-signals before the broader market reacts.