# Death And Taxes
An extremely configurable death tax plugin with Folia Support! Make death _cost._

## List of Features
- Percentage or fixed death taxes, with a protected minimum balance
- Configurable worlds, world blacklist mode, and permission-based discounts
- Tax-free grace deaths that persist on each player
- Optional server tax account for collected currency
- Configurable console and file logging for collected death taxes
- MiniMessage player messages and per-economy amount placeholders
- Folia & ShreddedPaper Support

## Requirements
DeathAndTaxes requires [Vault](https://github.com/MilkBowl/Vault) and an economy provider registered with Vault.

## Commands
- `/deathandtaxes reload` (alias: `/dat reload`) — reloads configuration from disk.

## Permissions
- `deathandtaxes.reload` - Reloads the plugin configuration
- `deathandtaxes.discount.X` - Applies an `X%` discount to death taxes when `X` is listed under `tax.discounts` in the config, using the highest granted configured value

## Configuration

### Economies
By default, the highest-priority Vault economy is taxed:

```yml
economy:
  default: true
```

To tax specific registered Vault economies instead, set `default: false` and list their provider names under `economy.economies`.

### Tax calculation

```yml
tax:
  mode: PERCENTAGE # PERCENTAGE or FIXED
  value: 10.0
  minimum-balance: 0.0
```

`PERCENTAGE` charges `value` percent of the current balance. `FIXED` charges `value` currency units. In both modes, the player keeps at least `minimum-balance`.

Taxes only apply in `tax.worlds`. Set `tax.blacklist-worlds: true` to tax every world except the listed worlds.

### Discounts
List the permitted percentages in `tax.discounts`, then grant players a permission such as `deathandtaxes.discount.25`. If a player has several configured discount permissions, the highest one applies.

### Grace deaths

```yml
grace-deaths:
  count: 1
```

Each player receives this number of tax-free deaths. Set it to `0` to disable the feature. Usage is stored in the player's persistent data and survives restarts and configuration reloads.

## Tax Account
Enable the account and provide a Vault account name to transfer each successfully collected tax instead of removing it:

```yml
tax:
  account:
    enabled: true
    name: "ServerTreasury"
    refund-on-failure: true
```

When disabled, taxes are removed as before. `refund-on-failure` controls whether a player is refunded when the destination account cannot receive the tax. A failed refund is logged as a severe error.

## Death-tax logs

```yml
logging:
  console: true
  file: true
```

Successful collections are logged with a UTC timestamp, player name and UUID, and the amount retained per economy. Console and file logging are enabled by default. The file is `plugins/DeathAndTaxes/death-taxes.log`.

## Messages and display

`display.decimal-places` controls the number of decimal places rendered in tax messages. Messages use [MiniMessage](https://docs.advntr.dev/minimessage/format.html) and may be blank to suppress them.

- `messages.death` supports `<amount>` for a single economy, or `<amount_economyname>` for each configured economy. Economy names are lowercased and non-alphanumeric characters become `_`.
- `messages.grace-death` supports the same amount placeholders, plus `<remaining>` and `<grace_deaths>`.
- `messages.discount` supports `<discount>`.

## Feature Requests & Contributing
If you'd like to contribute, simply make an issue with your proposed feature/enhancement or browse existing issues that are approved and make a pull request! PRs are accepted at this time.

## Support & Issues
Want to make a suggestion? Having an issue? Open a GitHub Issue [here](https://github.com/JosTheDude/death-and-taxes/issues) and let me know!

Found an exploit? Reach out ASAP to @josbot over Discord and let me know.
