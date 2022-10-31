'''
  Bot that checks for haikus in reddit comments. Inspired by the actual bot and coded by me
  Rules for haikus:
    1. There are 17 syllables in total.
    2. Haiku is composed of only 3 lines.
    3. Typically, every first line of Haiku has 5 syllables, the second line has 7 syllables, and the third has 5 syllables.
'''

import re
import nltk
from nltk.corpus import cmudict
import praw

pro = cmudict.dict()

reddit = praw.Reddit(
    client_id="not public",
    client_secret="not public",
    user_agent="<console:personal_weeb:1.0",
)

def count_syllables(word):
'''Return the number of syllables in a word

We first get the pronunciation of the word split in "phones"
Then we count how many phones include stress marks (basically nr of syllables)
'''
  word = re.sub(r'[^a-z ]+', '', word)
  return len([phone for phone in pro[word][0] if phone[len(phone) - 1].isdigit()])

def count_syllables_sentence(sentence):
'''Return a list of (word, syllables) tuples

We get the number of syllables in each word, if the total isnt 17, we can't make a haiku
'''
  words_and_syllables = []
  num_syllables = 0
  for word in sentence.lower().split():
    syllables = count_syllables(word)
    words_and_syllables.append((word, syllables))
    num_syllables += syllables
  if num_syllables == 17:
    return words_and_syllables
  
def construct_haiku(words_and_syllables):
'''Return a haiku given a list of (word, syllables) tuples
'''
  haiku = ""
  syllables_parsed = 0
  for word, syllable in words_and_syllables:
    haiku += word + " "
    syllables_parsed += syllable
    if syllables_parsed is 5 or syllables_parsed is 12:
      haiku += "\n"
  return haiku


def get_haiku(sentence):
""" Return a haiku given a sentence

Caller method for construct_haiku
"""
  words_and_syllables = count_syllables_sentence(sentence)
  if words_and_syllables is None:
    return
  else: return construct_haiku(words_and_syllables)

def get_haikus_from_subreddit(subreddit_name, num_posts):
'''Print haikus from subreddit comments
'''
  comments = []
    for submission in reddit.subreddit(subreddit_name).hot(limit=num_posts):
        for comment in submission.comments:
            if hasattr(comment, "body"):
              if all(x.isalpha() or x.isspace() for x in comment.body):
                get_haiku(comment.body)
                print()

def test_haiku():
'''Test our program with 2 sentences (1 which is a haiku and 1 which isnt)
'''
  print(get_haiku("This sentence can not be transformed into a haiku"))
  print(get_haiku("A summer river being crossed how pleasing with sandals in my hands!"))

if __name__ == '__main__':
  # get_haikus_from_subreddit("anime", 2)
  test_haiku()
