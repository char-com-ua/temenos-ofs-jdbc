
def ofs = "ENQUIRY.SELECT,,USER/PASS/,TEST"
def ofsResp = new File("snippets/enquiry_resp1.txt").getText("UTF-8")
def rq=com.temenos.tocf.ofsml.OfsmlRequest.parseOfs(ofs)

def rs = new com.temenos.tocf.ofsml.OfsmlEnquiryStandardResponse(rq)

rs.parseOfs(ofsResp.getBytes("UTF-8"))

println rs.saveString()
