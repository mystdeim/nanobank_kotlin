# NanoBank

## API

* GET /api/accounts -- show all existed accounts
* GET /api/account/:id -- get one account
* POST /api/account -- create account
* DELETE /api/account/:id -- delete account
* POST /api/transaction -- create transaction
* GET /api/account/:id/transactions -- get account's transactions

## Models

Account
```
{
  id: UUID
  person: String
  balance: BigDecimal
} 
```

Transaction
```
{
  id: UUID
  src: UUID
  dst: UUID
  vol: BigDecimal
} 
```

## How to deploy

```
gradle clean build
```

## Examples

```bash
# get accounts
curl -i -X GET http://localhost:8081/api/accounts 

# create account
curl -i -X POST http://localhost:8081/api/account -d '{"id":"c5198a64-a5a7-46a9-a0e3-c5cbc0f2f0ac","person":"Mike", "balance":"100.0"}'

# create account
curl -i -X POST http://localhost:8081/api/account -d '{"id":"8604882b-2830-41bf-9c0a-fa2f04c6db45","person":"Jack", "balance":"100.0"}'

# make transaction
curl -i -X POST http://localhost:8081/api/transaction -d '{"src":"c5198a64-a5a7-46a9-a0e3-c5cbc0f2f0ac","dst":"8604882b-2830-41bf-9c0a-fa2f04c6db45", "vol":"100.0"}'

# check account
curl -i -X GET http://localhost:8081/api/account/8604882b-2830-41bf-9c0a-fa2f04c6db45

# check transactions
curl -i -X GET http://localhost:8081/api/account/8604882b-2830-41bf-9c0a-fa2f04c6db45/transactions

# check account
curl -i -X GET http://localhost:8081/api/account/c5198a64-a5a7-46a9-a0e3-c5cbc0f2f0ac

# remove empty account
curl -i -X DELETE http://localhost:8081/api/account/c5198a64-a5a7-46a9-a0e3-c5cbc0f2f0ac

```