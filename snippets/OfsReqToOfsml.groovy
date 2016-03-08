def ofs = "ENQUIRY.SELECT,,USER/PASS/,ENQ.LWEXISTS"
def r=com.temenos.tocf.ofsml.OfsmlRequest.parseOfs(ofs)
println r.saveString()
