{#- Define the different Type column names we need -#}
{%- set sk_cols = ['tweet_id','player'] -%}

with v_tweets as
(
   select replace(id, '"', '')::BIGINT as tweet_id, replace(tweet, '"', '') as tweet, replace(UNNEST(players), '"', '') as player
   from {{ source('twitter_raw', 'tweets') }}
)
select {{ dbt_utils.generate_surrogate_key(sk_cols) }} as tweet_key, *
from v_tweets