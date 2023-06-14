{% snapshot scd_raw_players %}

{{
   config(
       target_schema='raw',
       unique_key='id',
       strategy='check',

       check_cols = ['name','team_id'],
       invalidate_hard_deletes=True
   )
}}

select * FROM {{ source('twitter_public', 'players') }}

{% endsnapshot %}