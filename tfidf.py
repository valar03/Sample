from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity

corpus = [
    "This is a sentence about cats.",
    "Dogs are also animals.",
    "I love my pet cat."
]

vectorizer = TfidfVectorizer()
X = vectorizer.fit_transform(corpus)

# Cosine similarity between sentence 0 and 2
similarity = cosine_similarity(X[0], X[2])
print(f"Similarity: {similarity[0][0]}")





####

from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity

corpus = [
    "how can I help you",
    "what assistance do you need"
]

vectorizer = TfidfVectorizer(stop_words='english', lowercase=True)
X = vectorizer.fit_transform(corpus)

similarity = cosine_similarity(X[0], X[1])
print(f"Similarity: {similarity[0][0]:.4f}")
