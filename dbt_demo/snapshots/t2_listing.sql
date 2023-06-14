{% snapshot t2_listing %}

{{
   config(
       target_schema='dbt',
       unique_key='listid',

       strategy='check',
       check_cols = ['sellerid','eventid','dateid']
   )
}}

{%- set t1_cols = ['numtickets','priceperticket','totalprice','listtime'] -%}

select listid::varchar || '-' || TO_CHAR(NOW()::timestamp, 'YYYYMMDDHH24MISS') as listing_key,
        *,
     {{ dbt_utils.generate_surrogate_key(t1_cols) }} as t1_key
from {{ source('tickit', 'listing') }}

{% endsnapshot %}