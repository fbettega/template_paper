library(tidyverse)



# extraction from pubmed location

path <- "../Data/ftp.ncbi.nlm.nih.gov/pubmed/baseline/"

expected_nb_of_files <- 1062

files_list <- list.files(path = path)
md5_files <- str_extract(files_list,".*\\.md5$") %>% na.omit()

xml_files <- str_extract(files_list,".*\\.xml\\.gz$") %>% na.omit()


if (length(md5_files) != expected_nb_of_files){
  cat("number of md5 files is not the expected number")
  }
if (length(xml_files) != expected_nb_of_files){
  cat("number of xml files is not the expected number")
}

md5_table <- lapply(md5_files, function(x) {
files_content <-  read_file(paste0(path,"\\",x[1]))

files_name <- str_match(files_content,"MD5\\(\\s*(.*?)\\s*\\)=")[,2]
md5_sum <- str_match(files_content,"=\\s{1}(.*?)\\n")[,2]

return(list(file = files_name,md5=md5_sum))
})  %>% bind_rows()

md5_table <- md5_table %>% 
  mutate(my_md5 = tools::md5sum(paste0(path,"\\",file )))



bad_hash <-  md5_table %>% mutate(bool = my_md5 == md5) %>% filter(!bool)
if (nrow(bad_hash) == 0){
  
lapply(xml_files,function(x){
R.utils::gunzip(paste0(path,"\\",x),destname= paste0("../Data/","temp/",str_remove(x,"\\.gz")), remove=FALSE)
}
)

zip(zipfile = paste0("../Data/","template_paper_data.zip"),list.files(path = "../Data/temp/",full.names = TRUE))
unlink("../Data/temp", recursive = TRUE)
}




