{% macro dim_update_timestamp(dim_name, inc_prefix, t2_prefix, source_prefix) %}
    {% if is_incremental() -%}
    {#- Incremental Run Case -#}
        case when ({{t2_prefix}}.dbt_valid_to is null
                    and {{inc_prefix}}.t1_key != {{source_prefix}}.t1_key) then NOW()::TIMESTAMP WITHOUT TIME ZONE
             when ({{inc_prefix}}.{{dim_name}}_key is not null) then {{inc_prefix}}.dbt_updated_at
             else {{t2_prefix}}.dbt_updated_at::timestamp end as dbt_updated_at
    {%- else %}
    {#- Initial Run Case -#}
        case when ({{t2_prefix}}.dbt_valid_to is null
                    and {{t2_prefix}}.t1_key != {{source_prefix}}.t1_key ) then NOW()::TIMESTAMP WITHOUT TIME ZONE
             else {{t2_prefix}}.dbt_updated_at::timestamp end as dbt_updated_at
    {%- endif %}
{% endmacro %}

