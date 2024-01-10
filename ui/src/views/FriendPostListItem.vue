<script lang="ts" setup>
import { VCard, VEntity, VEntityField,Dialog,VButton,VEmpty,VLoading,VPagination,Toast, VSpace, VDropdownItem} from "@halo-dev/components";
import { useQueryClient } from "@tanstack/vue-query";
import {useFriendPostFetch} from "@/api/use-friend";
import { ref } from "vue";
import service from "@/api/request";
import {FriendPost} from "@/types";
import { formatDatetime } from "@/utils/date";

const queryClient = useQueryClient();

const selectedFriendPosts = ref<string[]>([]);
const checkedAll = ref(false);

const page = ref(1);
const size = ref(20);
const keyword = ref("");
const searchText = ref("");

const { friendPosts, isLoading, total, refetch } = useFriendPostFetch(
  page,
  size,
  keyword
);

const handleCheckAllChange = (e: Event) => {
  const { checked } = e.target as HTMLInputElement;
  checkedAll.value = checked;
  if (checkedAll.value) {
    selectedFriendPosts.value =
      friendPosts.value?.map((friendPost) => {
        return friendPost.metadata.name;
      }) || [];
  } else {
    selectedFriendPosts.value.length = 0;
  }
};

const handleDeleteInBatch = () => {
  Dialog.warning({
    title: "是否确认删除所选的订阅帖子？",
    description: "删除之后将无法恢复。",
    confirmType: "danger",
    onConfirm: async () => {
      try {
        const promises = selectedFriendPosts.value.map((friendPost) => {
          return service.delete(`/apis/friend.moony.la/v1alpha1/friendPosts/${friendPost}`);
        });
        if (promises) {
          await Promise.all(promises);
        }

        selectedFriendPosts.value.length = 0;
        checkedAll.value = false;

        Toast.success("删除成功");
      } catch (e) {
        console.error(e);
      } finally {
        queryClient.invalidateQueries({ queryKey: ["friendPosts"] });
      }
    },
  });
};

</script>

<template>

  <VCard :body-class="['!p-0']">
    <template #header>
      <div class="block w-full bg-gray-50 px-4 py-3">
        <div class="relative flex flex-col items-start sm:flex-row sm:items-center" >
          <div class="mr-4 hidden items-center sm:flex" >
            <input
              v-model="checkedAll"
              class="h-4 w-4 rounded border-gray-300 text-indigo-600"
              type="checkbox"
              @change="handleCheckAllChange"
            />
          </div>
          <div class="flex w-full flex-1 items-center sm:w-auto" >
            <div class="flex items-center gap-2" >
              <FormKit
                v-if="!selectedFriendPosts.length"
                v-model="searchText"
                outer-class="!p-0"
                placeholder="输入关键词搜索"
                type="text"
                @keyup.enter="keyword = searchText"
              ></FormKit>
            </div>
            <VSpace v-if="selectedFriendPosts.length">
              <VButton type="danger" @click="handleDeleteInBatch">
                删除
              </VButton>
            </VSpace>
          </div>
        </div>
      </div>
    </template>
    <VLoading v-if="isLoading" />

    <Transition v-else-if="!friendPosts?.length" appear name="fade">
      <VEmpty
        message="暂无订阅帖子记录"
        title="暂无订阅帖子记录"
      >
        <template #actions>
          <VSpace>
            <VButton @click="refetch()"> 刷新 </VButton>
          </VSpace>
        </template>
      </VEmpty>
    </Transition>

    <Transition v-else appear name="fade">
      <div class="w-full relative overflow-x-auto">
        <table class="w-full text-sm text-left text-gray-500">
          <thead class="text-xs text-gray-700 uppercase bg-gray-50">
             <tr>
               <th scope="col" class="px-4 py-3"><div class="w-max flex items-center"> </div></th>
               <th scope="col" class="px-4 py-3"><div class="w-max flex items-center">名称 </div></th>
               <th scope="col" class="px-4 py-3"><div class="w-max flex items-center">站点链接 </div></th>
               <th scope="col" class="px-4 py-3"><div class="w-max flex items-center">帖子标题 </div></th>
               <th scope="col" class="px-4 py-3"><div class="w-max flex items-center">帖子链接 </div></th>
               <th scope="col" class="px-4 py-3"><div class="w-max flex items-center">帖子内容 </div></th>
               <th scope="col" class="px-4 py-3"><div class="w-max flex items-center">发布时间 </div></th>
             </tr>
          </thead>
          <tbody>
             <tr v-for="friendPost in friendPosts" class="border-b last:border-none hover:bg-gray-100">
               <td class="px-4 py-4">
                 <input
                   v-model="selectedFriendPosts"
                   :value="friendPost.metadata.name"
                   class="h-4 w-4 rounded border-gray-300 text-indigo-600"
                   name="post-checkbox"
                   type="checkbox"
                 />
               </td>
               <td class="px-4 py-4">{{friendPost.spec.author}}</td>
               <td class="px-4 py-4">
                 <a
                   :href="friendPost.spec.url"
                   class="hover:text-gray-900"
                   target="_blank"
                 >
                   {{ friendPost.spec.url }}
                 </a>
               </td>
               <td class="px-4 py-4">{{friendPost.spec.title}}</td>
               <td class="px-4 py-4">
                 <a
                   :href="friendPost.spec.link"
                   class="hover:text-gray-900"
                   target="_blank"
                 >
                   {{friendPost.spec.link}}
                 </a>
               </td>
               <td class="px-4 py-4">{{friendPost.spec.description}}</td>
               <td class="px-4 py-4">{{formatDatetime(friendPost.spec.pubDate)}}</td>
             </tr>
          </tbody>
        </table>
      </div>
      
<!--      <ul class="box-border h-full w-full divide-y divide-gray-100" role="list">-->
<!--        <li v-for="friendPost in friendPosts" >-->
<!--          <VEntity :is-selected="selectedFriendPosts.includes(friendPost.metadata.name)">-->
<!--            <template #checkbox>-->
<!--              <input-->
<!--                v-model="selectedFriendPosts"-->
<!--                :value="friendPost.metadata.name"-->
<!--                class="h-4 w-4 rounded border-gray-300 text-indigo-600"-->
<!--                name="post-checkbox"-->
<!--                type="checkbox"-->
<!--              />-->
<!--            </template>-->

<!--            <template #start>-->
<!--              <VEntityField :title="friendPost.spec.author">-->
<!--                <template #description>-->
<!--                  <a-->
<!--                    :href="friendPost.spec.url"-->
<!--                    class="truncate text-xs text-gray-500 hover:text-gray-900"-->
<!--                    target="_blank"-->
<!--                  >-->
<!--                    {{ friendPost.spec.url }}-->
<!--                  </a>-->
<!--                </template>-->
<!--              </VEntityField>-->
<!--              <VEntityField>-->
<!--                <div class="entity-field-description-body">-->
<!--                  <div class="flex flex-col gap-2">-->
<!--                    <div class="mb-1 flex items-center gap-2">-->
<!--                      <a-->
<!--                        :href="friendPost.spec.link"-->
<!--                        class="line-clamp-2 inline-block text-sm font-medium text-gray-900 hover:text-gray-600"-->
<!--                        target="_blank"-->
<!--                      >-->
<!--                        {{ friendPost.spec.title }}-->
<!--                      </a>-->
<!--                    </div>-->
<!--                  </div>-->
<!--                </div>-->
<!--              </VEntityField>-->
<!--              <VEntityField :title="friendPost.spec.description">-->
<!--              </VEntityField>-->
<!--            </template>-->

<!--            <template #end>-->
<!--              <VEntityField :description="formatDatetime(friendPost.spec.pubDate)"/>-->
<!--            </template>-->

<!--          </VEntity>-->
<!--        </li>-->
<!--      </ul>-->
    </Transition>

    <template #footer>
      <VPagination
        v-model:page="page"
        v-model:size="size"
        :total="total"
        :size-options="[20, 30, 50, 100]"
      />
    </template>
  </VCard>
  
</template>

<style scoped lang="scss">

</style>