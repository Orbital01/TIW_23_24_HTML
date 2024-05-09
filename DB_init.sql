drop database if exists TIW;
create database TIW;
use TIW;

create table users
(
    username varchar(50) primary key,
    password varchar(50),
    email    varchar(50) not null,
    nome     varchar(50),
    cognome  varchar(50)
);

create table gruppi
(
    ID         int auto_increment primary key,
    nome        varchar(50) unique not null,
    descrizione varchar(255),
    durata date,
    admin       varchar(50) not null,
    min_partecipanti int not null,
    max_partecipanti int not null,

    check ( min_partecipanti <= max_partecipanti),
    foreign key (admin) references users (username) on delete no action on update cascade
);

create table partecipation
(
    ID_gruppo int not null,
    user   varchar(50) not null,

    primary key (ID_gruppo, user),

    foreign key (ID_gruppo) references gruppi (ID) on delete cascade on update cascade,
    foreign key (user) references users (username) on delete no action on update cascade
);

-- aggiungo al database utenti e gruppi di prova
insert into users values
    ('user1', 'password1', 'user1@example.com', 'Nome1', 'Cognome1'),
    ('user2', 'password2', 'user2@example.com', 'Nome2', 'Cognome2'),
    ('user3', 'password3', 'user3@example.com', 'Nome3', 'Cognome3'),
    ('user4', 'password4', 'user4@example.com', 'Nome4', 'Cognome4'),
    ('user5', 'password5', 'user5@example.com', 'Nome5', 'Cognome5');

insert into gruppi (nome, descrizione, durata, admin, min_partecipanti, max_partecipanti) values
    ('Gruppo1', 'Descrizione1', '2023-12-01', 'user1', 1, 10),
    ('Gruppo2', 'Descrizione2', '2024-10-02', 'user2', 2, 20),
    ('Gruppo3', 'Descrizione3', '2024-02-03', 'user3', 3, 30),
    ('Gruppo4', 'Descrizione4', '2024-07-04', 'user4', 4, 40),
    ('Gruppo5', 'Descrizione5', '2024-06-05', 'user5', 5, 50),
    ('Gruppo6', 'Descrizione1', '2023-12-01', 'user5', 1, 10),
    ('Gruppo7', 'Descrizione2', '2024-10-02', 'user4', 2, 20),
    ('Gruppo8', 'Descrizione3', '2024-02-03', 'user3', 3, 30),
    ('Gruppo9', 'Descrizione4', '2024-07-04', 'user2', 4, 40),
    ('Gruppo10', 'Descrizione5', '2024-06-05', 'user1', 5, 50),
    ('Gruppo11', 'Descrizione1', '2025-12-01', 'user1', 1, 10),
    ('Gruppo12', 'Descrizione2', '2025-10-02', 'user2', 2, 20),
    ('Gruppo13', 'Descrizione3', '2025-02-03', 'user3', 3, 30),
    ('Gruppo14', 'Descrizione4', '2025-07-04', 'user4', 4, 40),
    ('Gruppo15', 'Descrizione5', '2025-06-05', 'user5', 5, 50);

INSERT INTO partecipation VALUES
    (1, 'user1'),
    (1, 'user2'),
    (2, 'user2'),
    (2, 'user3'),
    (3, 'user3'),
    (3, 'user4'),
    (4, 'user4'),
    (4, 'user5'),
    (5, 'user5'),
    (5, 'user1');
