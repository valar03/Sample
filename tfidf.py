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



######


synonym_dict = {
    "help": ["assist", "aid"],
    "assistance": ["help", "support"],
    "need": ["require", "want"]
}

def expand_sentence(sentence):
    words = sentence.lower().split()
    expanded = []
    for word in words:
        expanded.append(word)
        if word in synonym_dict:
            expanded.extend(synonym_dict[word])
    return " ".join(expanded)

sentences = [
    "How can I help you?",
    "What assistance do you need?"
]

expanded_sentences = [expand_sentence(s) for s in sentences]

# Now apply TF-IDF
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity

vectorizer = TfidfVectorizer(stop_words='english')
X = vectorizer.fit_transform(expanded_sentences)

sim = cosine_similarity(X[0], X[1])
print(f"Similarity: {sim[0][0]:.4f}")
