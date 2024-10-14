# Author: Kang Lin <kl222@126.com>

project(coturn)

# 添加子 cmakelists.txt
add_subdirectory(client)
add_subdirectory(server)
add_subdirectory(apps)

