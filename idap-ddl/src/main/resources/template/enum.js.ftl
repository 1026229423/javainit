import EsEnum from './esenum'
<#list list as c>
export const ${c.name} = new EsEnum([<#list c.values as v>{ name: '${v.name}', value: ${v.value}, desc: '${v.desc}' }<#if v_has_next>, </#if><#if (v_index+1)%4==0>
    </#if></#list>])
</#list>
