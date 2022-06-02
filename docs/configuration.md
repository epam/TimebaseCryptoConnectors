



# Configuration 

## Default 

By default, we launch with [default](https://raw.githubusercontent.com/epam/TimebaseCryptoConnectors/main/java/runner/src/main/docker/application.yaml) configuration to start all the available connectors.

## Custom 

You can override the [default](https://raw.githubusercontent.com/epam/TimebaseCryptoConnectors/main/java/runner/src/main/docker/application.yaml) configuration with your custom config. 

The default `application.yaml` is located in Docker in the [/runner/config](https://github.com/epam/TimebaseCryptoConnectors/blob/76ee7a34e1eaa0b68f36227d0ae19ff428ff6436/java/runner/src/main/docker/Dockerfile#L40) directory. To override this, you can mount the directory with your custom `application.yaml` config on the `/runner/config` directory as shown in the below example:  

```yaml
# docker-compose configuration
version: "3"
services:
  crypto-connectors:
    image: "epam/timebase-crypto-connectors:0.1.7"
    ports:
      - 8055:8055
    volumes:
      - /my/config_dir:/runner/config
```

For example, you can create a custom config to start just the selected data connector as shown in the below example:

```yaml
#application.yaml configuration
server:
  port: 8055

timebase:
  url: dxtick://timebase:8011

connectors:
  bitfinex:
    stream: bitfinex
    wsUrl: wss://api.bitfinex.com/ws/2
    depth: 20
    model:
      - "L1"
      - "L2"
      - "TRADES"
    instruments: "tBTCUSD=BTC/USD,tBTCEUR=BTC/EUR,tETCUSD=ETC/USD,tBTCF0:USTF0=BTCPC"
```

> For the convenience purposes, we recommend creating a `config` folder to store your custom configs in the `docker` directory, where the default application.yaml and the docker file are located.