{{
  config(
    materialized = 'incremental',
    on_schema_change='fail'
    )
}}
{#- Define the different Type column names we need -#}
{%- set sk_cols = ['p.id','p.name','t.team'] -%}

{% if is_incremental() %}
{%- set last_update_id = get_last_update( this ) -%}
{% endif %}

WITH raw_players AS (
    SELECT
        *
    FROM
        {{ ref('scd_raw_players') }}
)
SELECT
    {{ dbt_utils.generate_surrogate_key(sk_cols) }} as player_key,
    p.id as player_id, p.name as player_name, t.team,
    p.dbt_updated_at,p.dbt_valid_from, p.dbt_valid_to
FROM raw_players p LEFT JOIN  {{ source('twitter_public', 'teams') }} t ON(p.team_id = t.id)
{% if is_incremental() %}
WHERE dbt_updated_at > '{{ last_update_id }}'
{% endif %}