"""Trains, evaluates and exports the Qoody merchant classifier.

    python train.py            # train, print metrics, write the model, reference file and REPORT.md
    python train.py --dry-run  # train and print metrics only

Model: fastText-style. Hashed character n-grams, words and word pairs (see qmc.features) index an
embedding table; their mean goes through one linear layer and a softmax. Trained with plain SGD
and a linearly decaying learning rate, one example at a time, like fastText.
"""

from __future__ import annotations

import argparse
import json
import random
from pathlib import Path

import numpy as np

import dataset
import qmc

ROOT = Path(__file__).resolve().parents[2]
MODEL_OUT = ROOT / "mobile/androidApp/src/main/assets/merchant_classifier.bin"
REFERENCE_OUT = ROOT / "mobile/androidApp/src/test/resources/merchant_classifier_reference.json"
REPORT_OUT = Path(__file__).parent / "REPORT.md"

LABEL = "Qoody on-device v1"
"""Stored with every guess (Categorization.Model.modelName); change it when the model changes."""

TARGET_PRECISION = 0.95
"""The threshold is the lowest confidence at which accepted guesses reach this precision on validation."""

OTHER = dataset.CLASSES.index("Other")

# Extra reference strings for the Kotlin parity test: edge cases the test set lacks.
EDGE_CASES = ["", "   ", "1234", "@@@", "मिठाई भंडार", "a", "UPI/9876543210@ybl", "Ölmühle Café", "x" * 80]


def train(
    examples: list[dataset.Example],
    *,
    buckets: int,
    dim: int,
    min_n: int,
    max_n: int,
    epochs: int,
    lr: float,
    seed: int,
) -> qmc.Model:
    rng = np.random.default_rng(seed)
    classes = len(dataset.CLASSES)
    embeddings = rng.uniform(-1.0 / dim, 1.0 / dim, size=(buckets, dim)).astype(np.float32)
    weights = np.zeros((dim, classes), dtype=np.float32)
    bias = np.zeros(classes, dtype=np.float32)

    encoded = [(qmc.bucket_ids(e.text, min_n, max_n, buckets), dataset.CLASSES.index(e.label)) for e in examples]
    encoded = [(ids, y) for ids, y in encoded if ids]
    touched = np.zeros(buckets, dtype=bool)
    for ids, _ in encoded:
        touched[ids] = True

    order = list(range(len(encoded)))
    shuffle = random.Random(seed)
    total = epochs * len(encoded)
    step = 0
    for _ in range(epochs):
        shuffle.shuffle(order)
        for index in order:
            ids, y = encoded[index]
            rate = lr * (1.0 - step / total)
            step += 1
            hidden = embeddings[ids].mean(axis=0)
            logits = hidden @ weights + bias
            logits -= logits.max()
            probs = np.exp(logits)
            probs /= probs.sum()
            grad = probs
            grad[y] -= 1.0
            grad_hidden = weights @ grad
            weights -= rate * np.outer(hidden, grad)
            bias -= rate * grad
            np.subtract.at(embeddings, ids, rate * grad_hidden / len(ids))

    # Rows no training feature reached carry only their random start; zero them so unseen n-grams
    # add nothing (and the asset compresses well).
    embeddings[~touched] = 0.0
    return qmc.Model(LABEL, dataset.CLASSES, min_n, max_n, 0.0, embeddings, weights, bias)


def predictions(model: qmc.Model, examples: list[dataset.Example]) -> list[tuple[int, int, float]]:
    """(true class, predicted class, confidence) per example; no features predicts Other at 1.0."""
    out = []
    for e in examples:
        probs = model.probabilities(e.text)
        y = dataset.CLASSES.index(e.label)
        if probs is None:
            out.append((y, OTHER, 1.0))
        else:
            out.append((y, int(probs.argmax()), float(probs.max())))
    return out


def accepted_metrics(preds: list[tuple[int, int, float]], threshold: float) -> tuple[float, float, int]:
    """Precision of accepted guesses and coverage of categorisable examples at [threshold].

    A guess is accepted when it names a category (not Other) with confidence >= threshold.
    Coverage counts correct accepted guesses over examples whose true class is not Other.
    """
    accepted = [(y, p) for y, p, c in preds if p != OTHER and c >= threshold]
    correct = sum(1 for y, p in accepted if y == p)
    categorisable = sum(1 for y, _, _ in preds if y != OTHER)
    precision = correct / len(accepted) if accepted else 1.0
    coverage = correct / categorisable if categorisable else 0.0
    return precision, coverage, len(accepted)


def choose_threshold(preds: list[tuple[int, int, float]]) -> float:
    for step in range(30, 100):
        threshold = step / 100
        precision, _, accepted = accepted_metrics(preds, threshold)
        if accepted and precision >= TARGET_PRECISION:
            return threshold
    return 0.99


def confusion(preds: list[tuple[int, int, float]], threshold: float) -> list[list[int]]:
    """Rows: true class. Columns: predicted class after thresholding (rejected guesses count as Other)."""
    n = len(dataset.CLASSES)
    matrix = [[0] * n for _ in range(n)]
    for y, p, c in preds:
        matrix[y][p if c >= threshold else OTHER] += 1
    return matrix


def describe(name: str, preds: list[tuple[int, int, float]], threshold: float) -> list[str]:
    top1 = sum(1 for y, p, _ in preds if y == p) / len(preds)
    precision, coverage, accepted = accepted_metrics(preds, threshold)
    lines = [
        f"### {name} ({len(preds)} examples)",
        "",
        f"- Top-1 accuracy, no threshold: {top1:.3f}",
        f"- At threshold {threshold:.2f}: precision of accepted guesses {precision:.3f}, "
        f"coverage of categorisable merchants {coverage:.3f} ({accepted} guesses accepted)",
        "",
        "| confidence >= | precision | coverage | accepted |",
        "|---|---|---|---|",
    ]
    for t in (0.4, 0.5, 0.6, 0.7, 0.8, 0.9):
        p, c, a = accepted_metrics(preds, t)
        lines.append(f"| {t:.1f} | {p:.3f} | {c:.3f} | {a} |")
    short = ["Food", "Transp", "Shop", "Bills", "Subs", "Other"]
    lines += ["", "Confusion at the threshold (rows: true, columns: predicted; rejected guesses count as Other):", ""]
    lines.append("| | " + " | ".join(short) + " |")
    lines.append("|---" * (len(short) + 1) + "|")
    for name_, row in zip(short, confusion(preds, threshold)):
        lines.append(f"| {name_} | " + " | ".join(str(v) for v in row) + " |")
    lines.append("")
    return lines


def mistakes(examples: list[dataset.Example], preds: list[tuple[int, int, float]], threshold: float) -> list[str]:
    """Test entries the app would get wrong (wrong category) or leave Uncategorized."""
    wrong, missed = [], []
    for e, (y, p, c) in zip(examples, preds):
        guess = p if c >= threshold else OTHER
        if guess != y and guess != OTHER:
            wrong.append(f"- `{e.text}`: {dataset.CLASSES[y]} guessed as {dataset.CLASSES[guess]} ({c:.2f})")
        elif guess != y:
            missed.append(f"`{e.text}`")
    lines = ["Wrong categories on the test set:", ""] + (wrong or ["- none"]) + [""]
    lines += ["Left Uncategorized on the test set: " + (", ".join(missed) or "none") + ".", ""]
    return lines


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--seed", type=int, default=7)
    parser.add_argument("--buckets", type=int, default=1 << 16)
    parser.add_argument("--dim", type=int, default=16)
    parser.add_argument("--min-n", type=int, default=3)
    parser.add_argument("--max-n", type=int, default=5)
    parser.add_argument("--epochs", type=int, default=8)
    parser.add_argument("--lr", type=float, default=0.3)
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()

    train_set, validation = dataset.build(args.seed)
    test = dataset.load_test()
    print(f"train {len(train_set)}  validation {len(validation)}  test {len(test)}")

    settings = dict(
        buckets=args.buckets,
        dim=args.dim,
        min_n=args.min_n,
        max_n=args.max_n,
        epochs=args.epochs,
        lr=args.lr,
        seed=args.seed,
    )
    quantised, _, _ = qmc.quantise(train(train_set, **settings))
    threshold = choose_threshold(predictions(quantised, validation))

    report = [
        "# Merchant classifier report",
        "",
        "Generated by `train.py`; do not edit. All data is synthetic (see `dataset.py`).",
        "",
        f"Model `{LABEL}`: buckets {args.buckets}, dim {args.dim}, n-grams {args.min_n}-{args.max_n}, "
        f"epochs {args.epochs}, lr {args.lr}, seed {args.seed}. Embeddings int8-quantised; metrics use the "
        "quantised model, exactly as the app runs it.",
        "",
        f"Threshold {threshold:.2f}: the lowest confidence whose accepted guesses reach precision "
        f"{TARGET_PRECISION} on validation. Below it the app leaves the entry Uncategorized.",
        "",
        "## Evaluation model",
        "",
        "Trained without the validation names; used to pick the threshold.",
        "",
    ]
    report += describe("Validation (unseen stems, first names and brands)", predictions(quantised, validation), threshold)
    report += describe("Test, evaluation model", predictions(quantised, test), threshold)

    # The shipped model also learns the validation names; the threshold stays the validated one.
    model = train(train_set + validation, **settings)
    model.threshold = threshold
    shipped, _, _ = qmc.quantise(model)
    shipped_preds = predictions(shipped, test)
    report += [
        "## Shipped model",
        "",
        "Trained on training + validation data with the same settings and the threshold above. Only the "
        "hand-written test set is still unseen, so these are its numbers:",
        "",
    ]
    report += describe("Test (hand-written, data/test.tsv)", shipped_preds, threshold)
    report += mistakes(test, shipped_preds, threshold)
    report += [
        "In the app the keyword rules and remembered corrections run first, so well-known brands above "
        "(Swiggy, Flipkart, Netflix, LIC, ...) normally never reach the model.",
        "",
    ]
    print("\n".join(report))
    if args.dry_run:
        return

    MODEL_OUT.parent.mkdir(parents=True, exist_ok=True)
    written = qmc.write(model, str(MODEL_OUT))
    report.append(f"Model file: `{MODEL_OUT.relative_to(ROOT).as_posix()}`, {MODEL_OUT.stat().st_size} bytes.")
    REPORT_OUT.write_text("\n".join(report) + "\n", encoding="utf-8")

    reference = []
    for text in [e.text for e in test] + EDGE_CASES:
        probs = written.probabilities(text)
        reference.append({"text": text, "probabilities": None if probs is None else [float(p) for p in probs]})
    REFERENCE_OUT.parent.mkdir(parents=True, exist_ok=True)
    REFERENCE_OUT.write_text(
        json.dumps({"label": LABEL, "classes": dataset.CLASSES, "threshold": written.threshold, "cases": reference}, indent=1, ensure_ascii=False)
        + "\n",
        encoding="utf-8",
    )
    print(f"wrote {MODEL_OUT} ({MODEL_OUT.stat().st_size} bytes), {REFERENCE_OUT}, {REPORT_OUT}")


if __name__ == "__main__":
    main()
