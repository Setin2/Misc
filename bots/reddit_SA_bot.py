import praw
from nltk.sentiment.vader import SentimentIntensityAnalyzer

reddit = praw.Reddit(
    client_id="not public",
    client_secret="not public",
    user_agent="<console:personal_weeb:1.0",
)

sia = SentimentIntensityAnalyzer()

def get_subreddit_polarity(subreddit_name, num_posts):
""" Return te polarity score of a subreddit

Must specify how many posts to include 
"""
    comments = set()
    compound_polarity_scores = []
    for submission in reddit.subreddit(subreddit_name).hot(limit=num_posts):
        for comment in submission.comments:
            if hasattr(comment, "body"):
                comments.add(comment.body)

    for word in comments:
        pol_score = sia.polarity_scores(word)
        compound_polarity_scores.append(pol_score['compound'])
        
    overall_polarity_score = sum(compound_polarity_scores) / len(compound_polarity_scores)
    return overall_polarity_score

if __name__ == '__main__':
    sub_polarity = get_subreddit_polarity("anime", 10)
    print(sub_polarity)
