create table account (
    id UUID NOT NULL UNIQUE,
    person VARCHAR(255) NOT NULL,
    balance DECIMAL(20,2) NOT NULL
);
create table transaction (
    id UUID NOT NULL UNIQUE,
    src UUID NOT NULL,
    dst UUID NOT NULL,
    vol DECIMAL(20,2) NOT NULL
);