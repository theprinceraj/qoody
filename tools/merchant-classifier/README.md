# Merchant classifier (training tools)

Dev-only tooling for Qoody's on-device merchant classifier (decision D22, plan in
`docs/plans/merchant-classifier.md`). The app never runs Python: it ships the exported weights and
runs inference in Kotlin.

## What it produces

| File | What |
|---|---|
| `mobile/androidApp/src/main/assets/merchant_classifier.bin` | The model the app loads (QMC1 format, see `qmc.write`). |
| `mobile/androidApp/src/test/resources/merchant_classifier_reference.json` | Python's probabilities for the test set and edge cases; the Kotlin parity test must reproduce them. |
| `REPORT.md` | Metrics, threshold and the test set's mistakes for the committed model. |

All three are generated: change the code or data, rerun, commit them together. Training is
deterministic (fixed seed), so an unchanged rerun produces a byte-identical model.

## Run

```powershell
cd tools\merchant-classifier
python -m venv .venv
.venv\Scripts\python -m pip install -r requirements.txt
.venv\Scripts\python train.py --dry-run   # metrics only
.venv\Scripts\python train.py             # also writes the three files above
```

(macOS/Linux: `.venv/bin/python`.) Training takes well under a minute on a laptop.

## How it works

- `qmc.py`: the contract shared with Kotlin. Normalisation (ASCII lower case, digits kept and folded
  to `0`, everything else a separator), features (character 3–5-grams of `<word>`, the word, adjacent
  word pairs), 32-bit FNV-1a hashing into 65,536 buckets, mean of 16-dimensional embeddings, one
  linear layer, softmax. Embeddings are stored as int8 with one scale. **Change `qmc.py` and the
  Kotlin `classifier` package together**, and bump `FORMAT_VERSION` if the file layout changes.
- `dataset.py`: builds examples from `data/`. Brands (`brands.tsv`) and invented shop names
  (`stems.txt` x `keywords.tsv`, e.g. "Shree Ganesh Sweets"), plus person payees (`people.txt`) and
  generic businesses as class `Other`. Every example gets the same noise: UPI handles, upper case,
  truncation, prefixes, company suffixes, digits. Validation holds out whole brands, stems and first
  names.
- `train.py`: trains an evaluation model, picks the confidence threshold on validation (lowest
  confidence whose accepted guesses reach 0.95 precision), then trains the shipped model on all
  generated data with the same settings and reports it on `data/test.tsv`. That file is hand-written
  and never used for training.

Classes: `FoodAndDrink`, `Transport`, `Shopping`, `Bills`, `Subscriptions`, and `Other` (the app
keeps the entry Uncategorized). `Friends` is not predicted: a person's name does not tell a friend
from a small shop's personal UPI id.

## Data rules

Only public brand names and invented names. Never add real notification or SMS text, account
numbers, phone numbers or anyone's actual payees. To improve accuracy, add brands or business words
to `data/`, or add hard cases to `data/test.tsv` (and do not train on them).
