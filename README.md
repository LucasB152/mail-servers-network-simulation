# Mail Servers on a Simulated Network — SMTP, POP3, IMAP, DNS and OSPF

> University project · *Computer Networking* (University of Liège) · Java · Docker

A **mail server written from scratch in Java**, implementing **SMTP**, **POP3** and **IMAP**, deployed on a **simulated multi-domain network** built with Docker Compose. Each domain has its own subnet, DNS server and mail server. The subnets are connected by routers running **OSPF**, and mails between domains are forwarded using **DNS MX records**.

## Network topology

```
        info.uliege.be                 uliege.be                gembloux.uliege.be
        10.0.3.0/24                    10.0.1.0/24              10.0.2.0/24

  ┌─────────────────────┐        ┌─────────────────────┐        ┌─────────────────────┐
  │ dns_info   10.0.3.2 │        │ dns_uliege 10.0.1.2 │        │ dns_gembloux 10.0.2.2│
  │ mail_info  10.0.3.7 │        │ mail_uliege 10.0.1.7│        │ mail_gembloux 10.0.2.7│
  │ thunderbird 10.0.3.10│       │                     │        │                     │
  └──────────┬──────────┘        └──────────┬──────────┘        └──────────┬──────────┘
             │                              │                              │
       router_info ─────────────────────────┴───────────────── router_gembloux
    10.0.3.254 / 10.0.1.253          OSPF area 0            10.0.1.254 / 10.0.2.254
```

| Component | Technology |
|---|---|
| Mail servers | Java 17, implemented from scratch (sockets, thread pool) |
| DNS | BIND9: zones, NS, A and MX records |
| Routing | FRRouting (OSPF, area 0) |
| Mail client | Thunderbird, accessed in the browser |
| Orchestration | Docker Compose, 3 bridge networks with static addresses |

## Mail server

Each mail server serves one domain and listens on three ports:

| Protocol | Port | Supported commands |
|---|---|---|
| **SMTP** | 25 | Receives mails from clients and other servers, and forwards mails to other domains |
| **POP3** | 110 | `USER`, `PASS`, `STAT`, `LIST`, `RETR`, `DELE`, `CAPA`, `NOOP`, `QUIT` |
| **IMAP** | 143 | `LOGIN`, `CAPABILITY`, `LIST`, `SELECT`, `CREATE`, `DELETE`, `RENAME`, `APPEND`, `FETCH`, `STORE`, `UID`, `CHECK`, `EXPUNGE`, `CLOSE`, `NOOP`, `LOGOUT` |

- **Concurrency**: client connections are handled by a fixed-size thread pool. Its size is given at startup.
- **Protocol state machines**: each SMTP and POP3 session follows the states defined by the protocol, and commands sent in the wrong state are rejected.
- **Inter-domain forwarding**: when the recipient belongs to another domain, the SMTP server finds the destination mail server with a DNS MX lookup and forwards the mail to it.
- **Storage**: mailboxes are stored as JSON files, with the IMAP metadata (flags, UID, UIDVALIDITY).

## Repository structure

```
.
├── docker-compose.yml   # Whole network: routers, DNS, mail servers, Thunderbird
├── MailServer/          # Java mail server (SMTP, POP3, IMAP) and its Dockerfile
├── dns/                 # BIND9 configuration and zone file for each domain
└── frr/                 # OSPF configuration of the two routers
```

## Running

Requirements: Docker with Docker Compose.

```bash
docker compose up --build
```

Then open Thunderbird in your browser at **http://localhost:5800** and add an account. Test accounts (password `password`):

| uliege.be | info.uliege.be | gembloux.uliege.be |
|---|---|---|
| `dcd@uliege.be`, `vj@uliege.be` | `dcd@info.uliege.be`, `vj@info.uliege.be` | `dcd@gembloux.uliege.be`, `vj@gembloux.uliege.be` |

Send a mail from `dcd@info.uliege.be` to `vj@gembloux.uliege.be`: it goes through SMTP, the DNS MX lookup and the OSPF routers, and can then be read with POP3 or IMAP.
