"""Builds labelled merchant strings from the lexicons in data/.

Every example is synthetic: public brand names and invented shop and person names, never real
notifications. Noise (UPI handles, upper case, truncation, prefixes, company suffixes, digits) is
applied the same way to every class so it cannot become a category signal.

Splits are by name source, not by variant: a brand, stem or first name used for validation never
appears in training, so validation measures names the model has not seen.
"""

from __future__ import annotations

import random
from dataclasses import dataclass
from pathlib import Path

DATA = Path(__file__).parent / "data"

CLASSES = ["FoodAndDrink", "Transport", "Shopping", "Bills", "Subscriptions", "Other"]
"""Model classes in output order. "Other" means "no category" and becomes Uncategorized."""

UPI_HANDLES = ["ybl", "okaxis", "okhdfcbank", "oksbi", "okicici", "paytm", "apl", "ibl", "axl", "upi", "fbl", "pthdfc"]
PREFIXES = ["UPI-", "UPI/", "POS ", "To ", "Paid to ", "VPA ", "NEFT-", "IMPS/"]
SUFFIXES = [" Pvt Ltd", " Private Limited", " LLP", " India", " & Co", " and Sons", " Ltd"]
SURNAME_STEMS = {
    "Gupta", "Sharma", "Agarwal", "Jain", "Patel", "Shah", "Reddy", "Nair", "Iyer", "Singh", "Khan", "Das",
    "Ghosh", "Yadav", "Verma", "Mehta", "Joshi", "Desai", "Kumar", "Rao", "Pillai", "Menon", "Mishra",
    "Pandey", "Tiwari", "Chauhan",
}  # fmt: skip


@dataclass(frozen=True)
class Example:
    text: str
    label: str


def _lines(name: str) -> list[str]:
    rows = (DATA / name).read_text(encoding="utf-8").splitlines()
    return [r.strip() for r in rows if r.strip() and not r.startswith("#")]


def _pairs(name: str) -> list[tuple[str, str]]:
    pairs = []
    for row in _lines(name):
        label, text = row.split("\t", 1)
        assert label in CLASSES, f"{name}: unknown class {label!r}"
        pairs.append((label, text.strip()))
    return pairs


def load_test() -> list[Example]:
    return [Example(text, label) for label, text in _pairs("test.tsv")]


def _compact(text: str) -> str:
    return "".join(ch for ch in text.lower() if ch.isalnum())


def _variants(name: str, rng: random.Random, count: int) -> list[str]:
    """The clean name plus [count] noisy forms of it."""
    makers = [
        lambda: name.upper(),
        lambda: name.lower(),
        lambda: f"{_compact(name)}{rng.choice(['', '', str(rng.randint(1, 999))])}@{rng.choice(UPI_HANDLES)}",
        lambda: name.upper()[: rng.randint(10, 18)],
        lambda: rng.choice(PREFIXES) + rng.choice([name, name.upper()]),
        lambda: name + rng.choice(SUFFIXES),
        lambda: f"{name.upper()} {rng.randint(10, 999999)}",
        lambda: name.replace(" ", ""),
    ]
    out = [name]
    for maker in rng.sample(makers, k=min(count, len(makers))):
        out.append(maker())
    return out


def _split(items: list[str], rng: random.Random, fraction: float) -> tuple[list[str], list[str]]:
    shuffled = items[:]
    rng.shuffle(shuffled)
    cut = max(1, round(len(shuffled) * fraction))
    return shuffled[cut:], shuffled[:cut]


def build(seed: int, per_class: int = 3000, validation_fraction: float = 0.2) -> tuple[list[Example], list[Example]]:
    """Returns (train, validation) examples."""
    rng = random.Random(seed)
    brands = _pairs("brands.tsv")
    keywords = _pairs("keywords.tsv")
    stems = _lines("stems.txt")
    firsts = _lines("people.txt")

    train_stems, val_stems = _split(stems, rng, validation_fraction)
    train_firsts, val_firsts = _split(firsts, rng, validation_fraction)
    train_brands: dict[str, list[str]] = {}
    val_brands: dict[str, list[str]] = {}
    for label in CLASSES:
        names = [text for lab, text in brands if lab == label]
        if names:
            train_brands[label], val_brands[label] = _split(names, rng, validation_fraction)

    def make(stem_pool: list[str], first_pool: list[str], brand_pool: dict[str, list[str]], target: int) -> list[Example]:
        out: list[Example] = []
        surnames = [s for s in stem_pool if s in SURNAME_STEMS] or sorted(SURNAME_STEMS)
        for label in CLASSES:
            bases: list[str] = []
            words = [text for lab, text in keywords if lab == label]
            for brand in brand_pool.get(label, []):
                bases.extend([brand] * 3)
            while len(bases) < target // 3:
                if label == "Other" and rng.random() < 0.55:
                    bases.append(_person(rng, first_pool, surnames))
                elif label == "Other" and rng.random() < 0.15:
                    bases.append(" ".join(rng.sample(stem_pool, k=rng.randint(1, 2))))
                else:
                    bases.append(_shop(rng, stem_pool, words))
            for base in bases:
                out.extend(Example(text, label) for text in _variants(base, rng, count=2))
        rng.shuffle(out)
        return out

    train = make(train_stems, train_firsts, train_brands, per_class)
    validation = make(val_stems, val_firsts, val_brands, per_class // 4)
    return train, validation


def _shop(rng: random.Random, stems: list[str], words: list[str]) -> str:
    word = rng.choice(words)
    stem = " ".join(rng.sample(stems, k=rng.choice([1, 1, 2])))
    if word in {"Hotel", "Cafe", "Restaurant"} and rng.random() < 0.5:
        return f"{word} {stem}"
    return f"{stem} {word}"


def _person(rng: random.Random, firsts: list[str], surnames: list[str]) -> str:
    first, last = rng.choice(firsts), rng.choice(surnames)
    forms = [
        f"{first} {last}",
        f"{first} {last[0]}",
        f"{first[0]} {last}",
        f"Mr {first} {last}",
        f"Mrs {first} {last}",
        f"{first.lower()}.{last.lower()}{rng.randint(1, 99)}@{rng.choice(UPI_HANDLES)}",
        f"{rng.randint(6000000000, 9999999999)}@{rng.choice(UPI_HANDLES)}",
        f"{first} {rng.choice(firsts)} {last}",
    ]
    return rng.choice(forms)
