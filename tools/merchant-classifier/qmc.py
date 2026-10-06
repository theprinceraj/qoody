"""Qoody merchant classifier (QMC): the parts the Kotlin port must reproduce exactly.

Kotlin counterpart: mobile/shared/src/commonMain/kotlin/com/qoody/shared/capture/classifier/.
Anything changed here (normalisation, features, hash, file layout) must change there too,
and FORMAT_VERSION must be bumped when the file layout changes.
"""

from __future__ import annotations

import struct
from dataclasses import dataclass

import numpy as np

MAGIC = b"QMC1"
FORMAT_VERSION = 1

FNV_OFFSET = 0x811C9DC5
FNV_PRIME = 0x01000193


def normalise(text: str) -> str:
    """ASCII letters are lower-cased, digits kept, everything else becomes a space; spaces collapse."""
    out = []
    for ch in text:
        if "A" <= ch <= "Z":
            out.append(chr(ord(ch) + 32))
        elif "a" <= ch <= "z" or "0" <= ch <= "9":
            out.append(ch)
        else:
            out.append(" ")
    return " ".join("".join(out).split())


def features(text: str, min_n: int, max_n: int) -> list[str]:
    """Character n-grams of each "<word>", the word itself and adjacent word pairs.

    Digits are folded to "0" so reference numbers and VPA tails share features.
    """
    words = normalise(text).translate(DIGIT_FOLD).split(" ")
    words = [w for w in words if w]
    out: list[str] = []
    for word in words:
        padded = "<" + word + ">"
        for n in range(min_n, max_n + 1):
            for i in range(len(padded) - n + 1):
                out.append(padded[i : i + n])
        out.append("w " + word)
    for left, right in zip(words, words[1:]):
        out.append("b " + left + " " + right)
    return out


DIGIT_FOLD = str.maketrans("123456789", "000000000")


def fnv1a(feature: str) -> int:
    """32-bit FNV-1a over the feature's bytes (features are ASCII after normalisation)."""
    h = FNV_OFFSET
    for byte in feature.encode("ascii"):
        h ^= byte
        h = (h * FNV_PRIME) & 0xFFFFFFFF
    return h


def bucket_ids(text: str, min_n: int, max_n: int, buckets: int) -> list[int]:
    return [fnv1a(f) % buckets for f in features(text, min_n, max_n)]


@dataclass
class Model:
    label: str
    classes: list[str]
    min_n: int
    max_n: int
    threshold: float
    embeddings: np.ndarray  # float32 [buckets, dim]
    weights: np.ndarray  # float32 [dim, classes]
    bias: np.ndarray  # float32 [classes]

    @property
    def buckets(self) -> int:
        return self.embeddings.shape[0]

    @property
    def dim(self) -> int:
        return self.embeddings.shape[1]

    def probabilities(self, text: str) -> np.ndarray | None:
        """Softmax over [classes], or None when the text has no features (blank or non-Latin)."""
        ids = bucket_ids(text, self.min_n, self.max_n, self.buckets)
        if not ids:
            return None
        hidden = self.embeddings[ids].astype(np.float32).mean(axis=0)
        logits = hidden @ self.weights + self.bias
        logits = logits - logits.max()
        exp = np.exp(logits)
        return exp / exp.sum()


def quantise(model: Model) -> tuple[Model, np.ndarray, float]:
    """Int8 embeddings with one scale; returns the dequantised model plus what is written to disk."""
    peak = float(np.abs(model.embeddings).max())
    scale = peak / 127.0 if peak > 0 else 1.0
    q = np.clip(np.round(model.embeddings / scale), -127, 127).astype(np.int8)
    dequantised = Model(
        label=model.label,
        classes=model.classes,
        min_n=model.min_n,
        max_n=model.max_n,
        threshold=model.threshold,
        embeddings=q.astype(np.float32) * np.float32(scale),
        weights=model.weights.astype(np.float32),
        bias=model.bias.astype(np.float32),
    )
    return dequantised, q, scale


def write(model: Model, path: str) -> Model:
    """Writes the little-endian QMC1 file and returns the model exactly as the file encodes it.

    Layout:
      4s   magic "QMC1"
      u16  format version
      u8   label length, then label bytes (ASCII)
      u8   min n, u8 max n
      u32  buckets, u16 dim
      u8   class count, then per class: u8 length + ASCII name
      f32  confidence threshold
      f32  embedding scale
      i8   embeddings [buckets * dim], row-major
      f32  weights [dim * classes], row-major
      f32  bias [classes]
    """
    dequantised, q, scale = quantise(model)
    label = model.label.encode("ascii")
    with open(path, "wb") as f:
        f.write(MAGIC)
        f.write(struct.pack("<H", FORMAT_VERSION))
        f.write(struct.pack("<B", len(label)) + label)
        f.write(struct.pack("<BB", model.min_n, model.max_n))
        f.write(struct.pack("<IH", model.buckets, model.dim))
        f.write(struct.pack("<B", len(model.classes)))
        for name in model.classes:
            encoded = name.encode("ascii")
            f.write(struct.pack("<B", len(encoded)) + encoded)
        f.write(struct.pack("<ff", model.threshold, scale))
        f.write(q.tobytes(order="C"))
        f.write(model.weights.astype("<f4").tobytes(order="C"))
        f.write(model.bias.astype("<f4").tobytes(order="C"))
    dequantised.threshold = float(np.float32(model.threshold))
    return dequantised
