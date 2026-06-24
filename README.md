# Gym Service gRPC

A Spring Boot + gRPC microservice for managing gym operations: gym/member CRUD, member registration at gyms, visit check-in/check-out/cancel, and automated billing. Visit lifecycle drives billing — checking in opens a `Bill`, checking out closes it with a calculated amount (`daily_rate × duration`), cancelling produces a zero-value bill.

## Prerequisites

- Java 17
- `grpcurl`

## Build

```bash
./gradlew clean build
```

## Start the service

```bash
./gradlew bootRun
```

The gRPC server listens on `localhost:9090`.

Set a reusable host variable:

```bash
HOST=localhost:9090
```

---

## 1) Discover services

```bash
grpcurl -plaintext $HOST list
```

---

## Gym cycle

### Create gym

```bash
grpcurl -plaintext -d '{
  "name": "City Gym",
  "tax_id": "TAX789",
  "email": "info@citygym.com",
  "phone_number": "+49-555-9999",
  "website_url": "https://citygym.com",
  "capacity": 300,
  "address": {
    "street": "456 Oak Ave",
    "city": "Munich",
    "zip_code": "02101",
    "country": "Germany"
  }
}' $HOST com.demo.gym.GymService/CreateGym
```

### Get gym

```bash
grpcurl -plaintext -d '{"id": 1}' $HOST com.demo.gym.GymService/GetGym
```

### Modify gym

```bash
grpcurl -plaintext -d '{
  "id": 1,
  "name": "Updated Gym",
  "email": "new@email.com"
}' $HOST com.demo.gym.GymService/ModifyGym
```

---

## Member cycle

### Create member

```bash
grpcurl -plaintext -d '{
  "name": "John Doe",
  "tax_id": "TAX999",
  "email": "john@example.com",
  "phone_number": "555-1234",
  "address": {
    "street": "123 Main St",
    "city": "Boston",
    "zip_code": "02101",
    "country": "USA"
  },
  "birth_date": {
    "year": 1990,
    "month": 1,
    "day": 15
  },
  "sex": "SEX_MALE"
}' $HOST com.demo.gym.MemberService/CreateMember
```

### Get member

```bash
grpcurl -plaintext -d '{"id": 1}' $HOST com.demo.gym.MemberService/GetMember
```

### Modify member

```bash
grpcurl -plaintext -d '{
  "id": 1,
  "name": "John Smith",
  "email": "john.smith@example.com"
}' $HOST com.demo.gym.MemberService/ModifyMember
```

---

## Registration checks

### Register member in gym

```bash
grpcurl -plaintext -d '{
  "gym_id": 1,
  "member_id": 1
}' $HOST com.demo.gym.GymService/RegisterMember
```

### Get gym members

```bash
grpcurl -plaintext -d '{"id": 1}' $HOST com.demo.gym.GymService/GetGymMembers
```

### Get member gyms

```bash
grpcurl -plaintext -d '{"id": 1}' $HOST com.demo.gym.GymService/GetMemberGyms
```

---

## Visit cycle

### Create visit

```bash
grpcurl -plaintext -d '{
  "gym_id": 1,
  "member_id": 1,
  "zone": "Cardio",
  "station_code": "TM-01",
  "daily_rate": 150.0
}' $HOST com.demo.gym.VisitService/CreateVisit
```

### Get visit

```bash
grpcurl -plaintext -d '{"id": 1}' $HOST com.demo.gym.VisitService/GetVisit
```

### Get member visits

```bash
grpcurl -plaintext -d '{"member_id": 1}' $HOST com.demo.gym.VisitService/GetMemberVisits
```

### Check out visit

```bash
grpcurl -plaintext -d '{"id": 1}' $HOST com.demo.gym.VisitService/CheckOutVisit
```

---

## Bill checks

### List bill service methods

```bash
grpcurl -plaintext $HOST list com.demo.gym.BillService
```

### Describe bill service

```bash
grpcurl -plaintext $HOST describe com.demo.gym.BillService
```

### Get bill by id

```bash
grpcurl -plaintext -d '{"id": 1}' $HOST com.demo.gym.BillService/GetBill
```

### Get member bills

```bash
grpcurl -plaintext -d '{"member_id": 1}' $HOST com.demo.gym.BillService/GetMemberBills
```

### Get outstanding balance (member + gym)

```bash
grpcurl -plaintext -d '{"member_id": 1, "gym_id": 1}' $HOST com.demo.gym.BillService/GetOutstandingBalance
```
# gym-service
