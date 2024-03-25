This service is responsible for fetching the spot token price from Uniswap

Url example:
USDC/WETH - http://localhost:3001/price?token0=0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48&token1=0xc02aaa39b223fe8d0a0e5c4f27ead9083c756cc2&amount=100&depth=50
USDC/WBTC - http://localhost:3001/price?token0=0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48&token1=0x2260fac5e5542a773aa44fbcfedf7c193bc2c599&amount=100&depth=50

query params:
token0 - quote token
token1 - swapped token
amount - max amount for a levels
depth - levels quantity

How to start:
1. Install node
2. Specify blockchain_mainnet_node_url property in the config.js file
2. run commands from terminal:
>cd TimebaseCryptoConnectors/services/uniswap
>npm install
>node uniswap.js
