#include <stdio.h>
#include <stdlib.h>
 
#define MAX_TREE_HT 100
 
// Huffman tree node
struct MinHeapNode {
    char ch;
    unsigned freq;
    struct MinHeapNode *left, *right;
};
 
// Collection of Huffman tree nodes
struct MinHeap {
    unsigned size;
    unsigned capacity;
    struct MinHeapNode** array;
};
 
// Allocate a new min heap node with given character and frequency
struct MinHeapNode* newNode(char ch, unsigned freq)
{
    struct MinHeapNode* temp = (struct MinHeapNode*)malloc(sizeof(struct MinHeapNode));
 
    temp->left = temp->right = NULL;
    temp->ch = ch;
    temp->freq = freq;
 
    return temp;
}
 
// Create min heap of given capacity
struct MinHeap* createMinHeap(unsigned capacity){
    struct MinHeap* minHeap = (struct MinHeap*)malloc(sizeof(struct MinHeap));
 
    minHeap->size = 0;
    minHeap->capacity = capacity;
 
    minHeap->array = (struct MinHeapNode**)malloc(minHeap->capacity * sizeof(struct MinHeapNode*));
    return minHeap;
}
 
// Swap two min heap nodes
void swapMinHeapNode(struct MinHeapNode** a, struct MinHeapNode** b){
    struct MinHeapNode* t = *a;
    *a = *b;
    *b = t;
}
 
void minHeapify(struct MinHeap* minHeap, int idx){
 
    int smallest = idx;
    int left = 2 * idx + 1;
    int right = 2 * idx + 2;
 
    if (left < minHeap->size && minHeap->array[left]->freq < minHeap->array[smallest]->freq)
        smallest = left;
 
    if (right < minHeap->size && minHeap->array[right]->freq < minHeap->array[smallest]->freq)
        smallest = right;
 
    if (smallest != idx) {swapMinHeapNode(&minHeap->array[smallest], &minHeap->array[idx]);
        minHeapify(minHeap, smallest);
    }
}
 
int isSizeOne(struct MinHeap* minHeap){
    return (minHeap->size == 1);
}
 
struct MinHeapNode* extractMin(struct MinHeap* minHeap){
 
    struct MinHeapNode* temp = minHeap->array[0];
    minHeap->array[0] = minHeap->array[minHeap->size - 1];
 
    --minHeap->size;
    minHeapify(minHeap, 0);
 
    return temp;
}

void insertMinHeap(struct MinHeap* minHeap, struct MinHeapNode* minHeapNode){
    ++minHeap->size;
    int i = minHeap->size - 1;
 
    while (i && minHeapNode->freq < minHeap->array[(i - 1) / 2]->freq) {
        minHeap->array[i] = minHeap->array[(i - 1) / 2];
        i = (i - 1) / 2;
    }
 
    minHeap->array[i] = minHeapNode;
}
 
void buildMinHeap(struct MinHeap* minHeap){
    int n = minHeap->size - 1;
    int i;
 
    for (i = (n - 1) / 2; i >= 0; --i)
        minHeapify(minHeap, i);
}

void printArr(int arr[], int n){
    int i;
    for (i = 0; i < n; ++i)
        printf("%d", arr[i]);
 
    printf("\n");
}

int isLeaf(struct MinHeapNode* root){
    return !(root->left) && !(root->right);
}

struct MinHeap* createAndBuildMinHeap(char ch[], int freq[], int size){
    struct MinHeap* minHeap = createMinHeap(size);
 
    for (int i = 0; i < size; ++i)
        minHeap->array[i] = newNode(ch[i], freq[i]);
 
    minHeap->size = size;
    buildMinHeap(minHeap);
 
    return minHeap;
}
 
struct MinHeapNode* buildHuffmanTree(char ch[], int freq[], int size){
    struct MinHeapNode *left, *right, *top;
  
    struct MinHeap* minHeap = createAndBuildMinHeap(ch, freq, size);
 
    while (!isSizeOne(minHeap)) {
        // Extract the two minimum freq items from min heap
        left = extractMin(minHeap);
        right = extractMin(minHeap);
 
        // Create new node with frequency = sum of the two nodes frequencies.
        top = newNode('$', left->freq + right->freq);
 
        top->left = left;
        top->right = right;
 
        insertMinHeap(minHeap, top);
    }
  
    return extractMin(minHeap);
}
 
void printCodes(struct MinHeapNode* root, int arr[], int top){
    if (root->left) {
 
        arr[top] = 0;
        printCodes(root->left, arr, top + 1);
    }
 
    if (root->right) {
 
        arr[top] = 1;
        printCodes(root->right, arr, top + 1);
    }
 
    if (isLeaf(root)) {
 
        printf("%c: ", root->ch);
        printArr(arr, top);
    }
}
 
void HuffmanCodes(char ch[], int freq[], int size){
    struct MinHeapNode* root = buildHuffmanTree(ch, freq, size);
  
    int arr[MAX_TREE_HT], top = 0;
 
    printCodes(root, arr, top);
}
 
int main(){

    char arr[] = { 'a', 'b', 'c', 'd', 'e', 'f' };
    int freq[] = { 5, 9, 12, 13, 16, 45 };
 
    int size = sizeof(arr) / sizeof(arr[0]);
 
    HuffmanCodes(arr, freq, size);
 
    return 0;
}
