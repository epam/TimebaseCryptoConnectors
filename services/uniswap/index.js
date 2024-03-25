"use strict";
var __importDefault =
  (this && this.__importDefault) ||
  function (mod) {
    return mod && mod.__esModule ? mod : { default: mod };
  };
var express = require("express");
const { ethers } = require("ethers");
const { AlphaRouter } = require("@uniswap/smart-order-router");
const {
  Token,
  CurrencyAmount,
  TradeType,
  Percent,
} = require("@uniswap/sdk-core");
const JSBI = require("jsbi");
const lodash_1 = __importDefault(require("lodash"));
const config_1 = require("@uniswap/smart-order-router/build/main/routers/alpha-router/config");
const config = require("./config");

const provider = new ethers.providers.JsonRpcProvider(
  config.blockchain_mainnet_node_url
);
const tokenABI = [
  "function name() view returns (string)",
  "function symbol() view returns (string)",
  "function decimals() view returns (uint)",
];
const chainId = config.chainId;

var app = express();
app.listen(config.port, () => {
  console.log("Server running on port:", config.port);
});

app.get("/price", (req, res, next) => {
  try {
    getPriceSnapshot(req, res);
  } catch (e) {
    console.log("error", e);
  }
});

async function getPriceSnapshot(req, res) {
  let result = {};
  const token0Id = req.query.token0;
  const token1Id = req.query.token1;
  const maxAmount = req.query.amount ? req.query.amount.toString() : "20";
  const depth = req.query.depth ? req.query.depth.toString() : 20;

  if (token0Id && token0Id.length > 0 && token1Id && token1Id.length) {
    const token0Contract = new ethers.Contract(token0Id, tokenABI, provider);
    const token0Decimals = await token0Contract.decimals();
    const token0Symbol = await token0Contract.symbol();
    const token0Name = await token0Contract.name();

    const token1Contract = new ethers.Contract(token1Id, tokenABI, provider);
    const token1Decimals = await token1Contract.decimals();
    const token1Symbol = await token1Contract.symbol();
    const token1Name = await token1Contract.name();

    const token0 = new Token(
      chainId,
      token0Id,
      parseInt(token0Decimals),
      token0Symbol,
      token0Name
    );

    const token1 = new Token(
      chainId,
      token1Id,
      parseInt(token1Decimals),
      token1Symbol,
      token1Name
    );

    const router = new AlphaRouter({ chainId: chainId, provider: provider });

    let currencyAmount = CurrencyAmount.fromRawAmount(
      token1,
      JSBI.BigInt(ethers.utils.parseUnits(maxAmount, token1.decimals))
    );

    const blockNumber = router.getBlockNumberPromise();
    const routingConfig = lodash_1.default.merge(
      {},
      (0, config_1.DEFAULT_ROUTING_CONFIG_BY_CHAIN)(router.chainId),
      {},
      { blockNumber }
    );

    const { gasPriceWei } = await router.gasPriceProvider.getGasPrice();
    routingConfig.distributionPercent = 100 / depth;
    const [percents, amounts] = router.getAmountDistribution(
      currencyAmount,
      routingConfig
    );
    result["timestamp"] = Date.now();
    const bids = await getQuotes(
      gasPriceWei,
      percents,
      amounts,
      token0,
      token1,
      TradeType.EXACT_INPUT,
      router,
      routingConfig
    );

    const asks = await getQuotes(
      gasPriceWei,
      percents,
      amounts,
      token0,
      token1,
      TradeType.EXACT_OUTPUT,
      router,
      routingConfig
    );
    result["asks"] = asks;
    result["bids"] = bids;
  } else {
    result = { error: "Incorrect input values" };
  }
  return res.json(result);
}

async function getQuotes(
  gasPriceWei,
  percents,
  amounts,
  token0,
  token1,
  tradeType,
  router,
  routingConfig
) {
  let quotes = [];
  let tokenIn;
  let tokenOut;
  let quoteToken;
  quoteToken = token0;

  if (tradeType === TradeType.EXACT_OUTPUT) {
    tokenIn = token0;
    tokenOut = token1;
  } else {
    tokenIn = token1;
    tokenOut = token0;
  }

  const v3gasModel = await router.v3GasModelFactory.buildGasModel({
    chainId: router.chainId,
    gasPriceWei,
    v3poolProvider: router.v3PoolProvider,
    token: quoteToken,
    v2poolProvider: router.v2PoolProvider,
    l2GasDataProvider: router.l2GasDataProvider,
  });

  const quotePromises = [];
  quotePromises.push(
    router.getV3Quotes(
      tokenIn,
      tokenOut,
      amounts,
      percents,
      quoteToken,
      v3gasModel,
      tradeType,
      routingConfig
    )
  );
  quotePromises.push(
    router.getV2Quotes(
      tokenIn,
      tokenOut,
      amounts,
      percents,
      quoteToken,
      gasPriceWei,
      tradeType,
      routingConfig
    )
  );

  const routesWithValidQuotesByProtocol = await Promise.all(quotePromises);
  let allRoutesWithValidQuotes = [];
  let allCandidatePools = [];
  for (const {
    routesWithValidQuotes,
    candidatePools,
  } of routesWithValidQuotesByProtocol) {
    allRoutesWithValidQuotes = [
      ...allRoutesWithValidQuotes,
      ...routesWithValidQuotes,
    ];
    allCandidatePools = [...allCandidatePools, candidatePools];
  }
  const percentToQuotes = {};
  for (const routeWithValidQuote of allRoutesWithValidQuotes) {
    if (!percentToQuotes[routeWithValidQuote.percent]) {
      percentToQuotes[routeWithValidQuote.percent] = [];
    }
    percentToQuotes[routeWithValidQuote.percent].push(routeWithValidQuote);
  }

  const percentToSortedQuotes = lodash_1.default.mapValues(
    percentToQuotes,
    (routeQuotes) => {
      return routeQuotes.sort((routeQuoteA, routeQuoteB) => {
        if (tradeType == TradeType.EXACT_INPUT) {
          return by(routeQuoteA).greaterThan(by(routeQuoteB)) ? -1 : 1;
        } else {
          return by(routeQuoteA).lessThan(by(routeQuoteB)) ? -1 : 1;
        }
      });
    }
  );

  for (let i = 0; i < percents.length; i++) {
    const persentsToQuoteArray = percentToSortedQuotes[percents[i]];
    const bestQuote = persentsToQuoteArray[0];
    const size = parseInt(amounts[i].toExact());
    const quote = parseFloat(bestQuote.quote.toExact()) / size;
    const level = [quote, size];
    quotes.push(level);
  }
  return quotes;
}

const by = (rq) => rq.quoteAdjustedForGas;
