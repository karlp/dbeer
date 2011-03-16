<?xml version="1.0" encoding="UTF-8"?>
<bars>
    {% for bar in bars -%}
    <bar lat="{{bar.bar.location_geo[1]}}" lon="{{bar.bar.location_geo[0]}}" osmid="{{bar.bar.osmid}}">
        <name>{{bar.bar.name}}</name>
        <distance>{{bar.distance}}</distance>
        <prices>
            {%- for price in bar.prices -%}
            <price drinkid="{{price}}">{{bar.prices[price]}}</price>
            {%- endfor -%}
        </prices>
    </bar>
    {% endfor -%}
</bars>