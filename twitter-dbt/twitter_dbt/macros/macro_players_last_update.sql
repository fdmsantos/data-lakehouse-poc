{% macro get_last_update(table) %}
  {% if execute %}

    {% set query %}
        select max(dbt_updated_at) from {{table}}
    {% endset %}

    {%  set last_update_id = run_query(query).columns[0][0] %}

  {% else %}
    {% set last_update_id = -1 %}
  {% endif %}

  {% do return(last_update_id) %}
{% endmacro %}