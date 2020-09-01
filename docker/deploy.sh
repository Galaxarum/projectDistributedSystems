docker network create 'storage-net' --driver bridge

MACHINES=('leader' 'replica1')

for machine in "${MACHINES[@]}" ; do
    docker build -t "storage/$machine" -f "./application/$machine.dockerfile" "./application"
    docker run -d --network 'storage-net' --name "$machine" "storage/$machine"
    echo "---------------------"
done

docker build -t "storage/client" -f "./client/client.dockerfile" "./client/"
docker run -i -t --network 'storage-net' --name "client" "storage/client"