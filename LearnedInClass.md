# WEEK01_Lecture01_LLM:
1. $\text{Deep learning} \subset \text{Machine Learning}$.
2. in LLM. "Large" means both:
   1. model size in terms of parameters
   2. trained dataset size

3. Relationship:
![alt text](image.png)

4. Deep learning do not require manual feature extraction compared to ML.
5. General Process of creating LLM: petraining + fine tuning
   1. **pretraining**: train on large diverse datasets
   2. **fine tuning**: train on narrower dataset specific to particular tasks/domains.![alt text](image-1.png)
6. **Base/Foundation Model**: model after pretraining. Ex. GPT-3
7. Unlabled data for Pretraining and labeled data for Fine Tuning.
8. Fine Tuning:
   1. **Instruction Fine Tuning**: instruction + answer pairs
   2. **Classification Fine Tuning**: datasets with text and labels
9. **Transformer Architecture**: ![alt text](image-2.png)
10. **Self-attention mechanism**: allows the model to weigh the importance of different words or tokens in a sequence relative to each other.
11. **BERT**: bidirectional encoder representations from transformers.
12. **GPT**: generative pretrained transformers.![alt text](image-3.png)
13. BERT vs. GPT: 
    1.  BERT use Encoder and can reads text in both directions at the same time(to check both left and right to get better understand of current word related to the entire text).
    2.  GPT use Decoder and reads text only from left to right.(predicts the next word based only on previous words.)
14. **Zero-shot learning tasks**: ability to generalize to completely unseen tasks without any prior specific examples.
15. **Few-shot learning tasks**: Learning from a minimal number of examples the user provides as input.![alt text](image-4.png)
16. **Dolma**: An Open Corpus of Three Trillion Tokens for LLM Pretraining
17. The next-word prediction task is a form of self-supervised learning, which is a form of self-labeling.
18. **Autoregressive models(decoder-style models like GPT)**: unidirectional, left-to-right processing, incorporate their previous outputs as inputs for future predictions. ![alt text](image-5.png)
19. **Three main stages of coding an LLM:**
    1.   implementing the LLM architecture and data preparation process (stage 1)
    2.   pretraining an LLM to create a foundation model (stage 2)
    3.   fine-tuning the foundation model to become a personal assistant or text classifier (stage 3)![alt text](image-6.png)

# WEEK01_Lecture02_Text Data:
1. Vector Representation: ![alt text](image-7.png)
2. **Word2Vec**: words that appear in similar contexts
tend to have similar meanings.
3. Text processing steps in the context of an LLM: ![alt text](image-8.png)
4. Building a vocabulary by tokenizing the entire text![alt text](image-9.png)
5. **<|unk|>**：represent new and unknown words that were not part of the training data
6. **<|endoftext|>：** use to separate two unrelated text sources![alt text](image-10.png)
7. Some more special tokens: 
   1. [BOS] (beginning of sequence)：marks the start of a text.
   2. [EOS] (end of sequence)
   3. [PAD] (padding)： To ensure all texts have the same length, the shorter texts are extended or “padded” using the [PAD] token
8. **BPE(Byte-Pair Encoding)**: Iteratively merge the most frequent pair of consecutive bytes or characters in a text corpus until a predefined vocabulary size is reached.
9. Concepts related to BPE:
   1.  **Vocabulary**: A set of subword units that can be used to represent a text corpus.
   2.  **Byte**: A unit of digital information that typically consists of eight bits.
   3.  **Character**: A symbol that represents a written or printed letter or numeral.
   4.  **Frequency**: The number of times a byte or character occurs in a text corpus.
   5.  **Merge**: The process of combining two consecutive bytes or characters to create a new subword unit.
10. Example Steps for BPE: 
    1.  ![alt text](image-11.png)
    2.  ![alt text](image-12.png)
11. BPE tokenizer can break down unknow words into subwords, therefore it does not need to use special characters like <|unk|> to represent unknown words.
12. General steps: ![alt text](image-13.png)
13. Intrinsic vs Extrinsic Word Embeddings: 
    1.  Intrinsic:  Intrinsic evaluation measures the embedding quality directly, using tasks that test the vector properties themselves. It does not use a real downstream application. Example tests: Word similarity
    2.  Extrinsic: measures embeddings by using them inside a real task.

# Lecture3.1 - Neural_Approach_Word Embeddings:
1. **One-Hot Encoding**: Each word in the vocabulary is represented as a unique vector, where the dimensionality of the vector is equal to the size of the vocabulary. The vector has all elements set to 0, except for the element corresponding to the index of the word in the vocabulary, which is set to 1. There are **Disadvantages**: 
   1. too many dimension and high demand computation.
   2. each word is treated as an isolated entity without considering its meaning or context.
   3. restricted to the vocabulary seen during training making it unsuitable for handling out-of-vocabulary words.
2. **Bag of Word (Bow)**: It discards the word order and captures the frequency of each word in the document, creating a vector representation. ![alt text](image-14.png)Some **Disadvantages**: 
   1. ignore the sequence of words, leading to a loss of sequential information and context making it less effective for tasks where word order is crucial, such as in natural language understanding.
   2. reprentation often sparse, with many elements being zero resulting in increased memory requirements and computational inefficiency, especially when dealing with large datasets.
3. **TF-IDF(Term frequency-inverse document frequency)**: The higher the TF-IDF score for a term in a document, the more important that term is to that document within the context of the entire corpus.![alt text](image-15.png)
Some **Disadvantanges**:
   1. Treats words as independent entities and doesn’t consider semantic relationships between them.
   2. Sensitivity to Document Length, Longer documents tend to have higher overall term frequencies, potentially biasing TF-IDF towards longer documents.
   
# RAG:
1. Vector DB vs. Graph DB for RAG: ![alt text](image-16.png)
2. RAG pipeline: Ingestion, Retrieval and Synthesis.![alt text](image-17.png)
3. How to choose the size of chunk:
   1. Fixed-size chunking
   2. Context-aware chunking
   3. Naive Splitting
   4. NLTK (Natural Language Toolkit)
   5. spaCy
![alt text](image-18.png)
4. Two types of Retrieval:
   1. **Standard/Naive Approach**:
      1. Split documents into fixed chunks (e.g., 200–500 tokens)
      2. Convert each chunk into embeddings
      3. Store in vector DB

      At query time:
      1. Embed the query
      2. Retrieve top-k similar chunks

      **Advantages**:
      1. simply and efficient
      2. uniform and consistant in data handling

      **Disadvantages**:
      1. Limited Contextual Understanding
      2. Due to the limited context, the LLM might not have enough information to generate the most relevant and accurate responses.

   2. **Sentence-Window Retrieval**:
      1. Split text into small units (sentences)
      2. Store embeddings per sentence

      At retrieval:
      3. Find the most relevant sentence
      4. Expand it with neighboring sentences (window)

      **Advantages**:
      1. More precise retrieval of segments directly relevant to a query
      2. Broader understanding to formulate responses
      3. Balance between focused retrieval and contextual richness

      **Disadvantages**:
      1. More complex
      2. Risk of missing broader context if the surrounding information added back is not sufficiently comprehensive
5. **Retriever Ensembling**: Retriever ensembling = using multiple retrieval methods together and combining their results.
6. **Reranking**: Reranking takes those retrieved chunks and re-evaluates them more carefully.
   1. Lexical Re-Ranking: based on lexical similarity between the query and the retrieved documents.
   2. Semantic Re-Ranking: uses semantic understanding to judge the relevance of documents(like BERT).
   3. Learning-to-Rank (LTR) Methods: training a model specifically for the task of ranking documents based on features extracted from both the query and the documents.

# VectorDB
1. Traditional OLTP and OLAP Databases
   1. **OLTP**: used for day-to-day operations with lots of small, fast transactions(ex. MySQL, PostgreSQL).
   2. **OLAP**: used for analysis and reporting, often on large amounts of historical data.
2. Vector DB![alt text](image-19.png)
3. Three Key elements in Vector DB:
   1. **ID**: vector’s name tag
   2. **Dimensions**
   3. **Payload**: metadata
4. **Collection**: A container (or table) that stores vectors and their associated data.
5. **Sharding**: = a subset of your data.

   Problems without sharding:
      1. too much memory on one machine
      2. slow search
      3. poor scalability
      
      ![alt text](image-20.png)
6. **Distance Metrics**:
   1. **Euclidean Distance**: straight line distance 
   ![alt text](image-34.png)
   2. **Cosine Similarity**: The angle between two vectors, Ignores magnitude, focuses on direction
7. ![alt text](image-36.png)
   1. **Dot Product**: Combination of similarity in direction and magnitude of vectors 
   ![alt text](image-35.png)
   
# Graph RAG:
1. Use knowledge graphs to represent and connect information to capture not only more data points but also their relationships therefore it can uncover hidden connections that aren’t often obvious but are crucial for correlating information.
![alt text](image-21.png)
2. Graph RAG retrievers: 
![alt text](image-22.png)
3. Two steps to create knowledge graph:
   1. Model the relevant nodes and relationships to represent our domain data. Like Decide WHAT your graph should look like by defining Node types (entities), Relationship types, etc... ![alt text](image-23.png)
   2. Import, create, or compute the graph structures to fit this graph model like take real data and fill the graph.
4. **Lexical graph**: built from text-level relationships (words, co-occurrence, similarity)
5. **Domain graph**: built from real-world entities and meaningful relationships
6. **SimpleKGPipeline**:![alt text](image-24.png)

# SpringAI
1. Spring AI is an application framework for AI engineering.
2. ChatModel: ![alt text](image-25.png)
3. StreamingChatModel:![alt text](image-26.png)
4. Message interface: ![alt text](image-27.png)
5. MediaContent: ![alt text](image-28.png)

# FunctionCalling：
1. Tools - functionality we give the model
2. Tool calls - requests from the model to use tools
3. Tool call outputs - output we generate for the model. Can either be structured JSON or plain text, and it should contain a reference to a specific model tool call
4. Function vs. Tools: 
   1. A tool = any capability (API, database, calculator, etc.)
   2. A function = a specific kind of tool, defined by a JSON schema.
5. Tool Calling Flow: 
   1. Make a request to the model with tools it could call
   2. Receive a tool call from the model
   3. Execute code on the application side with input from the tool call
   4. Make a second request to the model with the tool output
   5. Receive a final response from the model (or more tool calls)
6. A function definition has the following properties:![alt text](image-29.png)

# ActorModel(partial before Midterm):
1. **Akka**: is a toolkit and runtime framework for building concurrent, distributed, and fault-tolerant message-driven applications on JVM.
2. **An Actor**: Each Actor owns 1 core.
 ![alt text](image-30.png)
3. When actor process a message, it can:
   1. change its state
   2. change its behavior for handling the next message
   3. create new actors
   4. send or reschedule messages to itself or other actors
   5. terminate
4. Benifits of Actor model:
   1. Conceptual simplicity
   2. high efficiency and safe concurrency
   3. high parrallelism
   4. easy to create highly resilient software
   5. easy to create distributed systems
5. Actors can:
   1. accumulate state in memory
   2. react to changes in state in real time
   3. no db calls, I/O, or external infrastructure
6. Actor Terms:
   1. **Actor**: a class that can contain state and modifies that state by processing messages it receives.
   2. **ActorReference** = a handle to an actor. Allows you to send the actor messages without knowing its implementation type or location on network.
   3. **ActorSystem**: = a collection of actors that exist inside a single process and communicate via in-memory message passing.
   4. **Cluster**: = a collection of networked ActorSystems whose actors communicate via TCP message passing
7. Actor Path: ![alt text](image-31.png)

# Threads:
1. 3 ways to create threads:
   1. implement Runnable Interface
   2. Extend Thread class
   3. Use Lambda Expressions
2. Comparison of 3 method: ![alt text](image-32.png)
3. Thread vs. Process in Java: ![alt text](image-33.png)
4. 

# Akka Actors in Java:
1. Typed vs. Classic:
   1. Typed: newer, more type safe. Messages sent to an actor are strictly defined by its type.
   2. older, less strictly typed. Actors can receive any type of message, requiring manual type checking through pattern matching. ![alt text](image-37.png)
2. Actor Characteristics: ![alt text](image-38.png)
3. System will already have 3 main actors:
   1. Root gardian actor: absolute root actor
   2. User gardian actor: where our created actors live
   3. System guardian actor: manage internal akka actor system.
4. How to create Actor class:
   1. Typed: ![alt text](image-40.png)
   2. Classic: ![alt text](image-39.png)
5. How to create actor:
   1. Typed: ![alt text](image-42.png)
   2. Classic: ![alt text](image-41.png)
6. How to import akka actor:
   1. Typed: with typed 
   2. Classic: without typed
7. props(): A factory method. Two ways to use:
   1. Customize: ![alt text](image-43.png)
      1. Create instance of this version: ![alt text](image-44.png)
   2. Direct Use:
      1. Create instance of this version: ![alt text](image-45.png)
8. Props: An immutable class.
9. Actor send messages:
   1.  tell(): send message with no expect of response. Usually 2 parameters(actual message(object), the sender actor ref).
       1.  Example: ![alt text](image-46.png) new OuterClass InnerClass() can create a new object/instance here is because the ReadLines() is a static class. If not, we need to do the traditional way which is: ![alt text](image-47.png)
       2.  ask(): send message and wait for response asynchronously. ask(actorRef, message, timeout).
           1.  Example: ![alt text](image-48.png) `CompletableFuture<Object>` is a placeholder for result that will arrive later.
       3.  forward(): similar to tell(), but the original sender is kept inside actor system. -->
10. receiveBuilder(): allow user to define message handling rules.
11. Kill Actor: system.stop(myActorRef). 
    1.  Stop is also asynchronously therefore it also follows FIFO rule.
    2.  stop a parent actor == stop all child actor that were spawned by it
    3.  no more incoming messages will be accepted
    
12. Terminate actor system:![alt text](image-49.png)
free up resources and prevent memory leaks. Also stop the system gardian actors.
1.   PoisonPill message: send message to kill other actors. ![alt text](image-50.png)
2.   Kill message: trigger failure and throw an ActorKilledException. So the supervisor will handle this.
3.   In conclusion: ![alt text](image-51.png)


# Interaction Patterns
1. Fire and forget: no guarantee the message is processed by other and no where to know wheter the message is received successfully. ![alt text](image-53.png)
2.  Request-Response: call a service and wait for result. Like ask(), this can only have 1 reply.
3. Adapted-Response: give them your address so they send result back
4. Request-Response vs. Adapted-Response: ![alt text](image-52.png)
5. Adapted-Response is better when 
   1. Translating between different actor message protocols: since different actors usually have its own message types. OtherResponse → WrappedResponse → handled by Actor A.
   2. Subscribing (many responses over time): send ONE request, get MANY responses. Like ![alt text](image-54.png)
   3. Disadvantages: ![alt text](image-55.png)
6. Request-Response in Typed Actor: Between 2 actors, interaction have 2 steps:
   1. need `ActorRef<Response> ` which is a `replyTo`.
   2. Transform response into your own message: wrap the reply
7. Advantages of Request-Response: ![alt text](image-56.png)
8. Disadvantages of Request-Response:![alt text](image-57.png)
9. Request-Response with ask from outside an Actor: Use fire and forget / ask().
   1.  Problems: ![alt text](image-58.png)
10. Ignoring replies: pass system.ignoreRef() if you are not interested in other actor's reply
    1.  Use: when Sending a message for which the protocol defines a reply, but you are not interested in getting the reply. Just like you must provide a replyTo, but you don’t care about the reply.
    2.  Disadvantages: ![alt text](image-59.png) 
    Actor A → sends replyTo (ignoreRef) → Actor B, Actor B → tries to reply → message disappears. Also ask() will never be acomplished therefore it never terminates.
11. Send Future result to self: 
    1.  Use: 
        1.  when actor A ask() get a future response from other actor(actor B), and inside actor A we want to use the response, since we cannot directly use future, we can convert it into a message and use it. Needs to Future → convert → message → actor handles it ![alt text](image-60.png)
        2.  Need replyTo/request ID
    2.  Disadvantages: have to write extra repetitive code just to wrap results into messages
12. Per session child Actor: A per-session child actor is when an actor creates another temporary actors to handle one specific task/request.
    1.  Disadvantages: ![alt text](image-61.png)
13. General purpose response aggregator: An actor that collects multiple responses from other actors and combines them into one final result.
    1.  Disadvantages: When building general-purpose aggregators: you want flexible message types but Java makes it harder to safely handle them.
14. Latency tail chopping: If a request is slow, send the same request to another actor (or server) and use whichever reply comes back first. Send "back up request" to another actor results in decreased response time since its less possible that all actors are heavy load.
    1.  Use: 
        1.  Reducing higher latency percentiles and variations of latency are important.
        2.  The “work” can be done more than once with the same result, e.g. a request to retrieve information.
    2.  Disadvantages: 
        1.  Increased load(more messages)
        2.  Can’t be used if the work can be only performed once
        3.  Message protocols with generic types are difficult since the generic types are erased in runtime
        4.  Children have life cycles that must be managed to not create a resource leak, it can be easy to miss a scenario where the session actor is not stopped
15. Summary of Interaction patterns:
    1.  Fire and Forget
    2.  Request-Response
    3.  Adapted-Response
    4.  Request-Response with ask between two actors
    5.  Request-Response with ask from outside an Actor
    6.  Ignoring replies
    7.  Send Future result to self
    8.  Per session child Actor
    9.  General purpose response aggregator
    10. Latency tail chopping


# FaultTolerance:
1. In typed,  they are stopped if an exception is thrown and no supervision strategy is defined.
2. In classic: restarted.
3. Fault avoidance strategy: ![alt text](image-62.png)
4. Exceptions are not good for recovering and continuing execution. They stop everything and undo or abort.
5. Actors that monitor other actors are called supervisors. Any actor that creates actors automatically supervision becomes the supervisor of those actors.
6. 4 options of supervior deal with failed actor:
   1. Restart : message cause fail lost, other still exists
   2. Resume: crash ignored, continue processing
   3. Stop: terminated
   4. Escalate: escalate to supervisor's supervisor
7. These are called let it crash strategy. Advantages: ![alt text](image-63.png)
8. Start event: 
   1. An actor is created and automatically started with the actorOf method.
   2. Top level actors are created with the actorOf method on the ActorSystem.
   3. A parent actor creates a child actor using the actorOf on its ActorContext.
9. Stop event:
   1.  indicates the end of the actor life-cycle and occurs once, when an actor is stopped.
   2.  An actor can be stopped using the stop method on the ActorSystem and ActorContext objects, or by sending a PoisonPill message to an actor.
10. Restart event:
    1. This can happen more than once, depending on the number of errors that occur.
    2. This event is a little bit more complex than the start or stop events. This is because the instance of an actor is replaced.
 11. Validation Error: the data of a command sent to an actor is not valid, this should rather be modelled as a part of the actor protocol than make the actor throw exceptions. Expected, normal, handled case, input is wrong.
 12. Failure: A failure is instead something unexpected or outside the control of the actor itself, for example a database connection that broke.
 13. Supervision: 
     1.  supervision allows you to declaratively describe what should happen when certain types of exceptions are thrown inside an actor.
     2.  can be further customized
     3.  the actual Actor behavior is wrapped using `Behaviors.supervise`.
     4.  Examples: 
         1.  ![alt text](image-64.png)
         2.  ![alt text](image-65.png)
         3.  ![alt text](image-66.png)
 14. Child actors are stopped(terminated) when parent is restarting: 
     1.  to avoid resource leak of creating new child actors each time the parent is restarted.
     2.  can be override so child actors stay alive when parent are restart
 15. PreRestart signal: Before a supervised actor is restarted it is sent the PreRestart signal giving it a chance to clean up resources it has created.
 16. Terminated = Child stopped(any reason)
 17. Child failed = stopped(failure)
 18. If child stops but parent does not handle it = parent crash(DeathPactException)
 19. Fault tolerance goals/benifits: ![alt text](image-67.png)
 20. Causes for actor failure while processing a message:
     1.  Programming error
     2.  Transient failure caused by an external resource used during processing the message
     3.  Corrupt internal state of the actor
 21. Two classes of supervision strategies:
     1.  OneForOneStrategy: applies to failed child only
     2.  AllForOneStrategy: applies to all children (if those children are closely related)
 22. Supervision vs.Watching: 
     1.  Supervision: Parent controls child (restart, stop, etc.)
     2.  Watching(DeathWatch): Just observes lifecycle (gets notified). Actor A depends on Actor B, if B stops, A receives Terminated(B)
 23. Hooks:
     1.  preStart: Right after starting the actor
     2.  postStop: After stopping an actor
     3.  preRestart: The actor is restarted when the preRestart callback function is invoked on the old actor. By default preRestart stops all children and calls postStop.
     4.  postRestart: The new actor’s postRestart callback method is invoked with the exception which caused the restart
 24. Exact Restart steps: ![alt text](image-69.png)


# Akka_Cluster
1. **Cluster singleton**: An actor that has only one instance in the entire cluster and is relocated to another node in case of failure.
2. **Cluster sharding**: Actors are automatically distributed to different nodes by a partition key, which is also used to communicate with the actors. This provides load balancing and resilience when the nodes leave the cluster.
3. **Distributed data**: Distributed key-value objects are ultimately consistent and can be used by any actor in any node of the cluster. The values are conflict-free replicated data types.
4. **Distributed publish-subscribe**: Actors can create, publish, and subscribe to topics.
5. **Reliable delivery**: Instead of the default at-most-once delivery, this module offers at-least-once guarantees.
6. **Multi-DC cluster**: you can deploy clusters in different data centers (DCs) to increase resilience.
7. Akka Management Tools:
   1. **Core (or Management)**: The basic tool that enables HTTP access.
   2. **Management Cluster HTTP**: A web API for your cluster
   3. **Cluster Bootstrap**: Automatically finds other nodes.
   4. **Discovery**: A service to find nodes.
8. **Cluster Membership**: Which nodes are part of the cluster
9. **Partition points**: You spread actors across multiple nodes so each node handles fewer actors
10. Seed nodes: A known node that new nodes contact first to join the cluster. Nodes join the cluster by sending a JOIN message containing the joining node’s unique address.
11. 3 ways to define seed node:
    1.  **Configuration**: manually write seed node addresses in config. not recommended for production.
    2.  **Use Cluster Bootstrap**: Nodes find each other automatically
    3.  **Programmatically**: You write your own logic to define seed nodes.
12. UP: A node is fully joined and active in the cluster.
13. **Configuration**: 
    1.  Other seed nodes can only join the cluster after the first seed node has successfully started and reached UP state. This restriction exists becasue we don't want other node automatically form new clusters.
14. Once the first seed node has formed the cluster and set all other seed nodes to Up, it can safely leave the cluster and pass its leadership to the next seed node in the list: that is, the one with the lowest IP address
15. Process of The first seed node leaves the cluster:
    1.  Seed 1 leaves cluster
    2.  seed 1 detected as unreachable
    3.  seed 2 becomes leader
16. **Gossip Protocal**: Every node gossips to other nodes about its own state and the states it has seen.
17. **Convergence**: The protocol makes it possible for all nodes in the cluster to eventually agree about the state of every node.
18. **Unreachable**: When a node is unreachable, cannot be set to leaving/UP(cannot assume), only state can be set is WeaklyUp(the node cannot be fully trust).
19. **Downing a reachable node**: It is possible to shut down a node from any node in the cluster by sending an `akka.cluster.type.Down` message.
    
# Akka_Sharding
1. Akka cluster sharding distributes the locations of actors, they are called entities when sharding.
2. Entity: A single logical actor identified by a unique ID
3. Relocating is useful: 
   1. reduce memory
   2. helps in failures: The entities are protected when a node fails by moving them to another healthy member of the cluster.
4. 4 type of actors used for Cluster Sharding:
   1. **Coordinator**: The manager of the whole system, only 1 per entity type in the entire cluster.
   2. **Shard Region**: A local manager on each node, Each node has one shard region.
   3. **Shard**: A group of entities.
   4. **Entity**: The actual actor YOU define. ![alt text](image-70.png)

# Persistence
1. CRUD: store current state only
2. Event sourcing: store every operation as an event
3. Use Persistence when: Actor:
   1. lives long
   2. has identity
   3. accumulates important data
4. Do not use Persistence when:
   1. the actor is stateless and can recompute everything from incoming messages
5. Event Sourcing Steps:
   1. Store events
   2. Recovery: apply every operation in order
6. **Journal**: interface, journal stores events in order and is immutable. avoids complexity and conflicts in concurrent systems.
7. Persistent Actor 2 modes:
   1. Recovery Mode: Replay events + rebuild state
   2. Command Mode: Receive commands (messages) + validate + produce events
8. **Snapshots**: State at time T, stored in a separate SnapshotStore.
   1. Advanatages: 
      1. speed up recovery
      2. avoid replaying all events


# Event Sourcing
1. CQRS: ommand Query Responsibility Segregation
2. Event characterisctics:
   1. past tense
   2. Immutable
   3. One-way: Publisher → subscribers
   4. Contains data(Contains real-life business intent meaning)
3. **Aggregates**: A group of related data treated as ONE unit. Has unique ID.Emits events.
4. **ORM**: 
   1. Load data from DB
   2. Modify object
   3. Save back
5. Advantages of Event Sourcing:
   1. Audit Trail: Full history
   2. Replay / Time Travel: reconstruct state at any time
   3. Debugging
   4. Performance: Append-only = fast writes
   5. Flexibility
6. Disadvantages:
   1. Performance (Read): Need replay → slow
   2. Versioning: Events change over time
   3. Querying
7. CQRS + Event Sourcing:
   1. Write Side: Handles commands + stores events
   2. Read Side: optimized for queries + uses projections
   3. Flow: Command → Event → Event Store → Read Model
8. When event happens → persist AND publish: Every event must:
   1. Be stored
   2. Be sent to others
   3. summary: must be persisted in the event store and published to other systems











# Additional:
1. **Database Sharding**: Splitting a large database into smaller pieces (called shards) and distributing them across multiple servers.
2. **4 NOSQL**: The four primary NoSQL database categories are Key-Value Stores, Document Databases, Column-Family Stores, and Graph Databases.
3. **Cypher**: Cypher is a graph query language used to query and manipulate graph databases.
4. Function vs. Method: 
   1. A function is a reusable piece of code that exists independently.
   2. A method is a function that belongs to a class or object.
5. anonymous inner class: is a class without a name that is declared and instantiated at the same time, usually used when you only need the class once
6. Stemming: Return words to their basic form.(apples -> apple)
7. ASCII Table: 
   1. Uppercase letters: from 65 to 90
   2. Lowercase Letters: from 97 to 122
   3. It use 7 bits to represent characters -> each bit is 0 or 1 -> total $2^7$ = 128
8. Regular Expressions: 
   1. .: any single characters
   2. +: 1 or more repetitions
   3. *: 0 or more repetitions
   4. ?: 0 or 1 repetition
   5. ^: not 
   6. {1,5}: range from 1 to 5 repetitions
   7. |: or
   8. ^ at the very front: start of string
   9. $: end of string
9.  MCP: Model Context Protocol. A standard way for applications to provide context tools, and data to an AI model.
10. Suppose there is a root actor with 2 child actors A and B, and actor A have its child actors AA and AB, so how to create an actor of AB(code): ![alt text](image-68.png)
11. CAP theorem: by Eric Brewer. A distributed system can guarantee only 2 out of these 3 properties at the same time:
    1.  C — Consistency: All users see the same data at the same time
    2.  A — Availability: System always responds (no errors)
    3.  P — Partition Tolerance: System continues working even if network breaks
12. Rank programing?
13. Range-Based Partitioning: split data into partitions based on value ranges
    1.  Example: Partition 1 → IDs 1 – 1000, Partition 2 → IDs 1001 – 2000, Partition 3 → IDs 2001 – 3000
14. Hash vs. Range-Based Partitioning:
    1.  Hash: use hash function to evenly distribute data
15. horizontal scaling and vertical scaling:
    1.  Horizontal scaling: Increase the power of ONE machine(Scale UP)
    2.  Vertical scaling: Increasing capacity by adding more machines to the system.\
16. Map Reduce:
    1.  **Map**: Split + Process, Break data into pieces across machines and each machine works independently
    2.  **Reduce**: Group same keys and combine values
17. memory cache system: Data can be accessed much faster than from disk or database. Just like when you search product on Amazon. Some searches shows very fast.


