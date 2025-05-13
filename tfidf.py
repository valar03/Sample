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


##faiss+tfidf

from sklearn.feature_extraction.text import TfidfVectorizer
import faiss
import numpy as np

# Sample corpus of sentences
sentences = [
    "I need help with my account",
    "How do I reset my password?",
    "Can I get assistance with login?",
    "What is the status of my refund?",
    "I would like to close my account"
]

# Step 1: Convert text to TF-IDF vectors
vectorizer = TfidfVectorizer()
tfidf_matrix = vectorizer.fit_transform(sentences)
tfidf_array = tfidf_matrix.toarray().astype('float32')

# Step 2: Create a FAISS index
dimension = tfidf_array.shape[1]
index = faiss.IndexFlatL2(dimension)
index.add(tfidf_array)

# Step 3: Query sentence
query = "I need assistance with logging in"
query_vec = vectorizer.transform([query]).toarray().astype('float32')

# Step 4: Search
k = 3  # Top 3 matches
distances, indices = index.search(query_vec, k)

# Step 5: Show results
print(f"Query: {query}\n")
print("Top similar sentences:")
for i in range(k):
    print(f"{i+1}. '{sentences[indices[0][i]]}' (Distance: {distances[0][i]:.4f})")
