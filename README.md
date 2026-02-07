# Bean & Brew – Smart Coffee Shop Queue Management

Hackathon-grade system for minimizing customer wait time with fairness and hard constraints.

## Tech Stack

- **Backend**: Java 17, Spring Boot 3.2, Maven
- **Database**: MySQL (MySQL Workbench)
- **Frontend**: React (Vite)
- **Architecture**: REST API + real-time simulation

## Business Rules

- **Drinks & Prep Times**: Cold Brew 1 min, Espresso/Americano 2 min, Cappuccino/Latte 4 min, Mocha 6 min
- **Hard Constraint**: No customer waits > 10 minutes
- **Urgency**: Customers frustrated after 8 min → +50 priority; alert at 9.5 min
- **Fairness**: If >3 later arrivals served first → apply penalty
- **Baristas**: 3 baristas, workload balancing (overloaded >1.2x prefers short jobs; underutilized <0.8x takes complex)

## Setup

### Prerequisites

- JDK 17
- Maven
- MySQL (e.g. via MySQL Workbench)
- Node.js 18+



