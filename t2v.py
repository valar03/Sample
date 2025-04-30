# rag_build_model_es.py
import json
from sentence_transformers import SentenceTransformer
from elasticsearch import Elasticsearch

# Load metadata
with open("image_metadata_numbered.json") as f:
    metadata = json.load(f)

texts = [
    f"{entry['description']} {' '.join(entry['tags'])}"
    for entry in metadata
]

model = SentenceTransformer("all-MiniLM-L6-v2")
vectors = model.encode(texts, convert_to_numpy=True).tolist()

# Connect to Elasticsearch
es = Elasticsearch("http://localhost:9200")  # Adjust to your ES address
index_name = "image_embeddings"

# Recreate index
if es.indices.exists(index=index_name):
    es.indices.delete(index=index_name)

es.indices.create(
    index=index_name,
    body={
        "mappings": {
            "properties": {
                "text": {"type": "text"},
                "embedding": {
                    "type": "dense_vector",
                    "dims": len(vectors[0]),
                    "index": True,
                    "similarity": "cosine"
                },
                "filename": {"type": "keyword"}
            }
        }
    }
)

# Index all vectors
for i, vec in enumerate(vectors):
    es.index(index=index_name, id=i, body={
        "text": texts[i],
        "embedding": vec,
        "filename": metadata[i]["filename"]
    })

es.indices.refresh(index=index_name)
print("✅ Done uploading to Elasticsearch.")


# rag_retrieval_custom_stock_imgs.py (FAISS replaced by Elasticsearch)

import json
import numpy as np
from transformers import pipeline
from elasticsearch import Elasticsearch

# Load metadata and texts
with open("embedding_texts.json") as f:
    texts = json.load(f)
with open("image_metadata_numbered.json") as f:
    metadata = json.load(f)

# Only used for offline embedding generation
# In restricted env: Load precomputed Elasticsearch index
es = Elasticsearch("http://localhost:9200")
index_name = "image_embeddings"

# Use same generator
generator = pipeline("text2text-generation", model="google/flan-t5-base")

def retrieve(query, k=3):
    # Use external script to embed and pass here (simulate query embedding)
    raise RuntimeError("Query embedding must be passed from outside.")

def retrieve_with_vector(query_vec, k=3):
    script_query = {
        "script_score": {
            "query": {"match_all": {}},
            "script": {
                "source": "cosineSimilarity(params.query_vector, 'embedding') + 1.0",
                "params": {"query_vector": query_vec}
            }
        }
    }

    res = es.search(index=index_name, body={
        "size": k,
        "query": script_query,
        "_source": ["text", "filename"]
    })

    docs = [hit["_source"]["text"] for hit in res["hits"]["hits"]]
    filenames = [hit["_source"]["filename"] for hit in res["hits"]["hits"]]
    return docs, filenames

def answer_with_rag(query, k=5):
    # You must replace this with a call to external embedding service
    raise RuntimeError("Call `answer_with_rag_vec(query_vec, k)` with vector instead.")

def answer_with_rag_vec(query_vec, k=5):
    docs, files = retrieve_with_vector(query_vec, k)
    context = "\n".join(docs)
    prompt = f"Context:\n{context}\n\nQuestion: {query}\nAnswer:"
    output = generator(prompt, max_new_tokens=150, do_sample=False)[0]['generated_text']
    return output, files

# For CLI test only (if you precompute vectors outside and paste)
if __name__ == "__main__":
    while True:
        vec = input("Paste query embedding vector (as Python list) or 'exit': ")
        if vec.strip().lower() == "exit":
            break
        vec = np.array(eval(vec)).tolist()
        ans, imgs = answer_with_rag_vec(vec)
        print("Images:", imgs)
        print("Answer:", ans)






@app.route('/img', methods=['POST'])
def generate_images():
    data = request.get_json()
    prompts = data.get('prompts', [])

    if not isinstance(prompts, list):
        return jsonify({"error": "Invalid input. 'prompts' should be a list."}), 400

    results = []
    for prompt in prompts:
        try:
            # ⛔️ Replace this with external embedding from client or script
            query_vec = get_vector_from_outside(prompt)  # You must implement this externally
            answer, image_result = answer_with_rag_vec(query_vec)
            results.append({"prompt": prompt, "result": image_result})
        except Exception as e:
            results.append({"prompt": prompt, "error": str(e)})

    return jsonify(results)
