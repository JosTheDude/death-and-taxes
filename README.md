# Death And Taxes
An extremely configurable death tax plugin with Folia Support! Make death _cost._

## List of Features
- Percentage & Fixed Loss of Currency on Death
- Optional Vault tax account for collected taxes
- Configurable tax-free grace deaths for new players
- Fully Configurable System + Messages
- Folia & ShreddedPaper Support

## Commands
- `/deathandtaxes reload` - Reloads the Configuration (`deathandtaxes.reload`)

## Permissions
- `deathandtaxes.reload` - Reloads the plugin configuration
- `deathandtaxes.discount.X` - Applies an `X%` discount to death taxes when `X` is listed under `tax.discounts` in the config, using the highest granted configured value

## Tax Account
Set `tax.account.enabled` to `true` and provide `tax.account.name` to deposit collected taxes into that Vault account. At startup, newly introduced config paths and their bundled comments are added without replacing existing values or comments. If an existing value conflicts with a newly introduced config section, the plugin leaves that value intact and logs a warning. `/deathandtaxes reload` applies edited values without writing the config file.

Set `tax.account.refund-on-failure` to control whether a player is refunded when the destination account cannot receive a tax.

## Feature Requests & Contributing
If you'd like to contribute, simply make an issue with your proposed feature/enhancement or browse existing issues that are approved and make a pull request! PRs are accepted at this time.

## Support & Issues
Want to make a suggestion? Having an issue? Open a GitHub Issue [here](https://github.com/JosTheDude/death-and-taxes/issues) and let me know!

Found an exploit? Reach out ASAP to @josbot over Discord and let me know.
