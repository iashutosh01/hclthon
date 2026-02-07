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

### Database

1. Create MySQL database `coffeeshop_queue` or let Spring Boot auto-create:
   ```sql
   CREATE DATABASE IF NOT EXISTS coffeeshop_queue;
   ```
2. Update `backend/src/main/resources/application.properties` if your MySQL user/password differ:
   ```properties
   spring.datasource.username=root
   spring.datasource.password=your_password
   ```

### Backend

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

Backend runs at http://localhost:8080

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend runs at http://localhost:5173 (proxies API to backend)

## REST APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /simulate/start | Start simulation (Poisson λ=1.4/min) |
| POST | /simulate/stop | Stop simulation |
| GET | /simulate/status | Get simulation status |
| GET | /queue | Get prioritized waiting queue |
| GET | /baristas | Get barista status & current orders |
| GET | /metrics | Get avg/max wait, timeout rate, fairness |
| POST | /orders | Manual order (body: customerName, drinkType, loyaltyStatus) |

## Priority Scoring (0–100)

- Wait Time: 40%
- Order Complexity: 25% (shorter prep = higher)
- Loyalty: 10% (Gold boost)
- Urgency: 25% (strong boost after 8 min)
- Emergency: +50 if wait > 8 min; force-assign near 10 min
- Fairness: -20 if laterArrivalsServedFirst > 3

## Project Structure

```
hclP2/
├── backend/
│   └── src/main/java/com/coffeeshop/
│       ├── controller/   SimulationController, OrderController
│       ├── service/      SchedulerService, PriorityService, SimulationService
│       ├── model/        Order, Barista, Assignment, Metrics
│       ├── repository/   OrderRepository, BaristaRepository
│       ├── dto/          OrderDTO, BaristaDTO, MetricsDTO
│       └── util/         PoissonGenerator, TimeUtils
└── frontend/
    └── src/
        ├── components/   QueueTable, BaristaPanel, MetricsDashboard
        ├── pages/        Dashboard
        └── services/     api.js
```

## Demo

1. Start MySQL, backend, and frontend.
2. Click **Start Simulation** – customers arrive via Poisson distribution.
3. Scheduler recalculates every 30 seconds; orders are assigned to baristas.
4. Use **Manual Order** to add specific orders for demos.
5. Watch **Metrics** (avg wait, max wait, timeout rate, fairness) and **Assignment Reason** for judges.
