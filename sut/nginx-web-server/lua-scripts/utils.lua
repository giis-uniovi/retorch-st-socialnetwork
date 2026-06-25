local _M = {}

_M.k8s_suffix = os.getenv("fqdn_suffix") or ""

function _M.is_empty(s)
  return s == nil or s == ''
end

return _M
